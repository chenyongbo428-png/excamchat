package com.cheat.exam.service.model;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼 / 通义千问 OpenAI-compatible 适配器。
 *
 * 当前先实现非流式 chat completions，输出统一转为 ModelChatResponse。
 * 后续如果接入流式输出，只需要在同一供应商适配器旁边扩展 stream 方法即可。
 */
@Component
public class BailianModelClient implements ModelClient {

    private static final String PROVIDER_CODE = "BAILIAN";
    private static final String DEFAULT_GUIDANCE_STAGE = "observe";
    private static final String DEFAULT_TEACHER_INTENT = "guide_next_step";
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern JSON_INT_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(\\d+)");
    private static final Pattern JSON_BOOLEAN_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    private final AppProperties.Bailian properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public BailianModelClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this(appProperties, objectMapper, HttpClient.newHttpClient());
    }

    BailianModelClient(AppProperties appProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = appProperties.getModel().getBailian();
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean supports(ModelClientSelection selection) {
        return PROVIDER_CODE.equalsIgnoreCase(selection.providerCode());
    }

    @Override
    public ModelChatResponse chat(ModelChatRequest request) {
        ensureConfigured();
        Map<String, Object> requestBody = buildRequestBody(request);
        String responseBody = sendRequest(requestBody);
        return parseResponse(request, responseBody);
    }

    @Override
    public ModelChatResponse stream(ModelChatRequest request, Consumer<String> chunkConsumer) {
        ensureConfigured();
        ModelChatRequest streamRequest = new ModelChatRequest(
            request.sessionId(),
            request.modelCode(),
            request.providerCode(),
            request.subjectCode(),
            request.gradeLevel(),
            request.systemPrompt(),
            request.currentUserMessage(),
            request.image(),
            request.messages(),
            request.guidanceMode(),
            true
        );
        Map<String, Object> requestBody = buildRequestBody(streamRequest);
        return sendStreamRequest(streamRequest, requestBody, chunkConsumer);
    }

    private void ensureConfigured() {
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new ApiException(
                "MODEL_CONFIG_MISSING",
                "BAILIAN_API_KEY is required before using Bailian/Qwen models",
                HttpStatus.BAD_REQUEST
            );
        }
        if (StringUtils.isBlank(properties.getBaseUrl())) {
            throw new ApiException(
                "MODEL_CONFIG_MISSING",
                "BAILIAN_BASE_URL is required before using Bailian/Qwen models",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private Map<String, Object> buildRequestBody(ModelChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.modelCode());
        body.put("messages", buildMessages(request));
        body.put("temperature", properties.getTemperature());
        // Qwen-VL 当前 OpenAI 兼容接口对 max_tokens 支持更稳定，避免长 JSON 被截断后解析失败。
        body.put("max_tokens", properties.getMaxTokens());
        if (request.streamRequested()) {
            body.put("stream", true);
        }
        return body;
    }

    private List<Map<String, Object>> buildMessages(ModelChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(request)));

        List<ModelMessageInput> history = request.messages() == null ? List.of() : request.messages();
        int historyLimit = Math.max(0, history.size() - 12);
        for (int i = historyLimit; i < history.size(); i++) {
            ModelMessageInput item = history.get(i);
            if (isCurrentUserMessage(item, request.currentUserMessage())) {
                continue;
            }
            String role = toProviderRole(item.roleCode());
            if (role == null || StringUtils.isBlank(item.contentText())) {
                continue;
            }
            messages.add(Map.of("role", role, "content", item.contentText()));
        }

        List<Map<String, Object>> currentContent = new ArrayList<>();
        currentContent.add(Map.of("type", "text", "text", buildCurrentUserText(request)));
        if (request.image() != null) {
            currentContent.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", toImageUrl(request.image()))
            ));
        }
        messages.add(Map.of("role", "user", "content", currentContent));
        return messages;
    }

    private String buildSystemPrompt(ModelChatRequest request) {
        String businessPrompt = StringUtils.defaultIfBlank(request.systemPrompt(), "你是一位耐心的引导型老师。");
        if ("direct".equalsIgnoreCase(StringUtils.trimToEmpty(request.guidanceMode()))) {
            return businessPrompt + """

                当前启用“直答模式”：请识别题目图片，直接给出题目内容、正确答案和完整但清晰的解题过程。
                不要输出 JSON，不要输出 Markdown 代码块，不要生成画布标注协议。
                """;
        }
        return """
            %s

            现在启用“引导式讲解模式”，请严格遵守：
            1. 不要在第一轮直接给最终答案、完整证明或完整解题过程。
            2. 首轮只做三件事：识别题意，列出关键已知条件，提出一个学生可以立刻回答的小问题。
            3. 多轮对话时，先判断学生上一条回答是否正确；正确则推进下一小步，错误则指出卡点并给一个更具体的提示。
            4. 只有当学生已经完成关键推理、连续多轮卡住，或明确要求总结时，才可以给阶段性总结；即便总结也要先解释思路，再给结论。
            5. 请不要输出 JSON，不要输出 Markdown 代码块，不要生成画布标注协议；直接输出学生可读的中文自然语言。
            6. 每次回复尽量短一些，聚焦一个问题或一个提示，避免一次讲完。
            """.formatted(businessPrompt);
    }

    private String buildCurrentUserText(ModelChatRequest request) {
        String userMessage = StringUtils.defaultIfBlank(request.currentUserMessage(), "请讲解这道题。");
        long userMessageCount = countPreviousUserMessages(request.messages(), request.currentUserMessage());
        boolean directMode = "direct".equalsIgnoreCase(StringUtils.trimToEmpty(request.guidanceMode()));
        String guidanceInstruction = directMode
            ? "这是直答模式：请直接给出识别结果、正确答案和完整解题过程。"
            : userMessageCount <= 0
            ? "这是本题第一轮讲解：请先识别题目和关键条件，只提出一个引导问题，不要给最终答案。"
            : "这是多轮追问：请先判断学生这次回答/问题，再给下一步提示，不要直接跳到最终答案。";
        return """
            学科：%s
            年级：%s
            已有学生发言轮数：%d
            学生问题：%s

            %s
            """.formatted(
            StringUtils.defaultIfBlank(request.subjectCode(), "GENERAL"),
            StringUtils.defaultIfBlank(request.gradeLevel(), "UNKNOWN"),
            userMessageCount,
            userMessage,
            guidanceInstruction
        );
    }

    private long countPreviousUserMessages(List<ModelMessageInput> messages, String currentUserMessage) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream()
            .filter(item -> "USER".equalsIgnoreCase(item.roleCode()))
            // SessionService 会先保存当前用户消息，再构造模型请求，这里排除当前轮避免首轮被误判为多轮。
            .filter(item -> !StringUtils.equals(
                StringUtils.trimToEmpty(item.contentText()),
                StringUtils.trimToEmpty(currentUserMessage)
            ))
            .count();
    }

    private boolean isCurrentUserMessage(ModelMessageInput item, String currentUserMessage) {
        return "USER".equalsIgnoreCase(item.roleCode())
            && StringUtils.equals(StringUtils.trimToEmpty(item.contentText()), StringUtils.trimToEmpty(currentUserMessage));
    }

    private String toProviderRole(String roleCode) {
        if ("USER".equalsIgnoreCase(roleCode)) {
            return "user";
        }
        if ("ASSISTANT".equalsIgnoreCase(roleCode)) {
            return "assistant";
        }
        return null;
    }

    private String toImageUrl(ModelImageInput image) {
        if (StringUtils.isNotBlank(image.storageKey())) {
            // 本地上传图片外部模型无法访问站内相对 URL，因此转为 data URL 直接随请求发送。
            return toDataUrl(image.storageKey(), image.mimeType());
        }
        if (StringUtils.isNotBlank(image.accessUrl()) && image.accessUrl().startsWith("http")) {
            return image.accessUrl();
        }
        throw new ApiException("IMAGE_NOT_AVAILABLE", "Image content is not available for model call", HttpStatus.BAD_REQUEST);
    }

    private String toDataUrl(String storageKey, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(storageKey));
            String contentType = StringUtils.defaultIfBlank(mimeType, "image/png");
            return "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException ex) {
            throw new ApiException("IMAGE_NOT_FOUND", "Image file not found for model call", HttpStatus.NOT_FOUND);
        }
    }

    private String sendRequest(Map<String, Object> requestBody) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(resolveChatUri())
                .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                    "MODEL_PROVIDER_ERROR",
                    "Bailian model call failed with HTTP " + response.statusCode(),
                    HttpStatus.BAD_GATEWAY
                );
            }
            return response.body();
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to serialize Bailian request", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException ex) {
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to call Bailian model", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("MODEL_PROVIDER_ERROR", "Bailian model call was interrupted", HttpStatus.BAD_GATEWAY);
        }
    }

    private URI resolveChatUri() {
        String baseUrl = StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()), "/");
        if (baseUrl.endsWith("/chat/completions")) {
            return URI.create(baseUrl);
        }
        if (baseUrl.endsWith("/compatible-mode/v1")) {
            return URI.create(baseUrl + "/chat/completions");
        }
        return URI.create(baseUrl + "/compatible-mode/v1/chat/completions");
    }

    private ModelChatResponse sendStreamRequest(
        ModelChatRequest request,
        Map<String, Object> requestBody,
        Consumer<String> chunkConsumer
    ) {
        StringBuilder fullText = new StringBuilder();
        String providerRequestId = null;
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(resolveChatUri())
                .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                    "MODEL_PROVIDER_ERROR",
                    "Bailian model stream failed with HTTP " + response.statusCode(),
                    HttpStatus.BAD_GATEWAY
                );
            }

            // OpenAI-compatible 流式响应按 data: JSON 分行推送，[DONE] 表示结束。
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String data = StringUtils.removeStart(line.trim(), "data:").trim();
                    if (StringUtils.isBlank(data)) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode chunk = objectMapper.readTree(data);
                    if (providerRequestId == null) {
                        providerRequestId = chunk.path("id").asText(null);
                    }
                    String delta = chunk.path("choices").path(0).path("delta").path("content").asText("");
                    if (StringUtils.isNotEmpty(delta)) {
                        fullText.append(delta);
                        chunkConsumer.accept(delta);
                    }
                }
            }
            return toPlainTextResponse(request, fullText.toString(), providerRequestId);
        } catch (JsonProcessingException ex) {
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to parse Bailian stream response", HttpStatus.BAD_GATEWAY);
        } catch (IOException ex) {
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to call Bailian model stream", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException("MODEL_PROVIDER_ERROR", "Bailian model stream was interrupted", HttpStatus.BAD_GATEWAY);
        }
    }

    private ModelChatResponse toPlainTextResponse(ModelChatRequest request, String content, String providerRequestId) {
        Map<String, Object> businessPayload = fallbackPayload(content);
        return new ModelChatResponse(
            providerCode(),
            request.modelCode(),
            StringUtils.defaultIfBlank(content, "模型没有返回有效内容，请稍后重试。"),
            intValue(businessPayload.get("hintLevel"), 1),
            StringUtils.defaultIfBlank(stringValue(businessPayload.get("guidanceStage")), DEFAULT_GUIDANCE_STAGE),
            StringUtils.defaultIfBlank(stringValue(businessPayload.get("teacherIntent")), DEFAULT_TEACHER_INTENT),
            booleanValue(businessPayload.get("shouldRevealFinalAnswer"), false),
            listOfMapValue(businessPayload.get("annotations")),
            businessPayload,
            providerRequestId
        );
    }

    private ModelChatResponse parseResponse(ModelChatRequest request, String responseBody) {
        try {
            JsonNode providerPayload = objectMapper.readTree(responseBody);
            String providerRequestId = providerPayload.path("id").asText(null);
            String content = providerPayload.path("choices").path(0).path("message").path("content").asText("");
            Map<String, Object> rawPayload = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            Map<String, Object> businessPayload = parseBusinessPayload(content);

            String replyText = stringValue(businessPayload.get("replyText"));
            if (StringUtils.isBlank(replyText)) {
                replyText = StringUtils.defaultIfBlank(content, "我暂时没有生成有效讲解，请换个问法或稍后重试。");
            }

            List<Map<String, Object>> annotations = listOfMapValue(businessPayload.get("annotations"));
            Map<String, Object> mergedPayload = new LinkedHashMap<>();
            mergedPayload.putAll(businessPayload);
            mergedPayload.put("rawProviderPayload", rawPayload);

            return new ModelChatResponse(
                providerCode(),
                request.modelCode(),
                replyText,
                intValue(businessPayload.get("hintLevel"), 1),
                StringUtils.defaultIfBlank(stringValue(businessPayload.get("guidanceStage")), DEFAULT_GUIDANCE_STAGE),
                StringUtils.defaultIfBlank(stringValue(businessPayload.get("teacherIntent")), DEFAULT_TEACHER_INTENT),
                booleanValue(businessPayload.get("shouldRevealFinalAnswer"), false),
                annotations,
                mergedPayload,
                providerRequestId
            );
        } catch (JsonProcessingException ex) {
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to parse Bailian response", HttpStatus.BAD_GATEWAY);
        }
    }

    private Map<String, Object> parseBusinessPayload(String content) {
        String json = extractJsonObject(content);
        if (StringUtils.isBlank(json)) {
            return fallbackPayload(content);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            // 真实模型偶尔会返回被截断或带多余字段的 JSON；讲解文本不能因此整轮失败。
            return fallbackPayload(content);
        }
    }

    private Map<String, Object> fallbackPayload(String content) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("replyText", fullReplyText(firstJsonStringField(content, "replyText", content)));
        fallback.put("guidanceStage", firstJsonStringField(content, "guidanceStage", DEFAULT_GUIDANCE_STAGE));
        fallback.put("hintLevel", firstJsonIntField(content, "hintLevel", 1));
        fallback.put("teacherIntent", firstJsonStringField(content, "teacherIntent", DEFAULT_TEACHER_INTENT));
        fallback.put("shouldRevealFinalAnswer", firstJsonBooleanField(content, "shouldRevealFinalAnswer", false));
        fallback.put("annotations", List.of());
        fallback.put("parseFallback", true);
        return fallback;
    }

    private String firstJsonStringField(String content, String fieldName, String fallback) {
        Matcher matcher = Pattern.compile(JSON_STRING_FIELD_PATTERN.pattern().formatted(Pattern.quote(fieldName))).matcher(
            StringUtils.defaultString(content)
        );
        if (!matcher.find()) {
            return fallback;
        }
        return unescapeJsonString(matcher.group(1));
    }

    private int firstJsonIntField(String content, String fieldName, int fallback) {
        Matcher matcher = Pattern.compile(JSON_INT_FIELD_PATTERN.pattern().formatted(Pattern.quote(fieldName))).matcher(
            StringUtils.defaultString(content)
        );
        if (!matcher.find()) {
            return fallback;
        }
        return intValue(matcher.group(1), fallback);
    }

    private boolean firstJsonBooleanField(String content, String fieldName, boolean fallback) {
        Matcher matcher = Pattern.compile(JSON_BOOLEAN_FIELD_PATTERN.pattern().formatted(Pattern.quote(fieldName)), Pattern.CASE_INSENSITIVE)
            .matcher(StringUtils.defaultString(content));
        if (!matcher.find()) {
            return fallback;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private String unescapeJsonString(String value) {
        try {
            return objectMapper.readValue("\"" + value + "\"", String.class);
        } catch (JsonProcessingException ex) {
            return value;
        }
    }

    private String fullReplyText(String content) {
        // 这里是用户最终看到并入库的答案，不能做摘要截断；截断只应出现在日志等非用户展示场景。
        return StringUtils.defaultIfBlank(content, "模型没有返回有效内容，请稍后重试。").trim();
    }

    private String extractJsonObject(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return trimmed.substring(start, end + 1);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMapValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }
}
