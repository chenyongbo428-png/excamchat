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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ArkModelClient implements ModelClient {

    private static final Logger log = LoggerFactory.getLogger(ArkModelClient.class);
    private static final String PROVIDER_CODE = "ARK";
    private static final String DEFAULT_GUIDANCE_STAGE = "observe";
    private static final String DEFAULT_TEACHER_INTENT = "guide_next_step";

    private final AppProperties.Ark properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ArkModelClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this(appProperties, objectMapper, HttpClient.newHttpClient());
    }

    ArkModelClient(AppProperties appProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = appProperties.getModel().getArk();
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
        log.info(
            "Calling Ark model: modelCode={}, sessionId={}, mode={}, streamRequested={}, hasImage={}, messageCount={}",
            request.modelCode(),
            request.sessionId(),
            request.guidanceMode(),
            request.streamRequested(),
            request.image() != null,
            request.messages() == null ? 0 : request.messages().size()
        );
        String responseBody = sendRequest(buildRequestBody(request));
        ModelChatResponse response = parseResponse(request, responseBody);
        log.info(
            "Ark model call completed: modelCode={}, sessionId={}, providerRequestId={}, replyLength={}",
            response.modelCode(),
            request.sessionId(),
            response.providerRequestId(),
            response.replyText() == null ? 0 : response.replyText().length()
        );
        return response;
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
        return sendStreamRequest(streamRequest, buildRequestBody(streamRequest), chunkConsumer);
    }

    private void ensureConfigured() {
        if (StringUtils.isBlank(properties.getApiKey())) {
            throw new ApiException(
                "MODEL_CONFIG_MISSING",
                "ARK_API_KEY is required before using Doubao/Ark models",
                HttpStatus.BAD_REQUEST
            );
        }
        if (StringUtils.isBlank(properties.getBaseUrl())) {
            throw new ApiException(
                "MODEL_CONFIG_MISSING",
                "ARK_BASE_URL is required before using Doubao/Ark models",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private Map<String, Object> buildRequestBody(ModelChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.modelCode());
        body.put("messages", buildMessages(request));
        body.put("temperature", properties.getTemperature());
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
        int historyStart = Math.max(0, history.size() - 12);
        for (int i = historyStart; i < history.size(); i++) {
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
        if (request.image() != null) {
            currentContent.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", toImageUrl(request.image()))
            ));
        }
        currentContent.add(Map.of("type", "text", "text", buildCurrentUserText(request)));
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
            4. 只有当学生已经完成关键推理、连续多轮卡住，或明确要求总结时，才可以给阶段性总结。
            5. 请不要输出 JSON，不要输出 Markdown 代码块，不要生成画布标注协议；直接输出学生可读的中文自然语言。
            6. 每次回复聚焦一个问题或一个提示，避免一次讲完。
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
            log.info("Preparing Ark image input from local file: imageId={}, mimeType={}, storageKeyPresent=true", image.imageId(), image.mimeType());
            return toDataUrl(image.storageKey(), image.mimeType());
        }
        if (StringUtils.isNotBlank(image.accessUrl()) && image.accessUrl().startsWith("http")) {
            log.info("Preparing Ark image input from remote URL: imageId={}, mimeType={}", image.imageId(), image.mimeType());
            return image.accessUrl();
        }
        throw new ApiException("IMAGE_NOT_AVAILABLE", "Image content is not available for model call", HttpStatus.BAD_REQUEST);
    }

    private String toDataUrl(String storageKey, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(Path.of(storageKey));
            String contentType = StringUtils.defaultIfBlank(mimeType, "image/png");
            log.info("Ark image encoded as data URL: mimeType={}, bytes={}", contentType, bytes.length);
            return "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(bytes));
        } catch (IOException ex) {
            log.warn("Ark image file not found or unreadable: storageKey={}", storageKey);
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
            log.info("Ark HTTP response received: status={}, bodyLength={}", response.statusCode(), response.body() == null ? 0 : response.body().length());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Ark HTTP error response: status={}, body={}", response.statusCode(), truncateForLog(response.body()));
                throw new ApiException(
                    "MODEL_PROVIDER_ERROR",
                    providerErrorMessage("Ark model call failed", response.statusCode(), response.body()),
                    HttpStatus.BAD_GATEWAY
                );
            }
            return response.body();
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize Ark request: {}", ex.getMessage());
            throw new ApiException("INTERNAL_ERROR", "Failed to serialize Ark request", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException ex) {
            log.warn("Failed to call Ark model: {}", ex.getMessage());
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to call Ark model", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Ark model call was interrupted");
            throw new ApiException("MODEL_PROVIDER_ERROR", "Ark model call was interrupted", HttpStatus.BAD_GATEWAY);
        }
    }

    private URI resolveChatUri() {
        String baseUrl = StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()), "/");
        if (baseUrl.endsWith("/chat/completions")) {
            return URI.create(baseUrl);
        }
        return URI.create(baseUrl + "/chat/completions");
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
            log.info("Ark stream HTTP response received: status={}", response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String responseBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("Ark stream HTTP error response: status={}, body={}", response.statusCode(), truncateForLog(responseBody));
                throw new ApiException(
                    "MODEL_PROVIDER_ERROR",
                    providerErrorMessage("Ark model stream failed", response.statusCode(), responseBody),
                    HttpStatus.BAD_GATEWAY
                );
            }

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
                    JsonNode deltaNode = chunk.path("choices").path(0).path("delta");
                    String delta = deltaNode.path("content").asText("");
                    if (StringUtils.isEmpty(delta)) {
                        delta = deltaNode.path("reasoning_content").asText("");
                    }
                    if (StringUtils.isNotEmpty(delta)) {
                        fullText.append(delta);
                        chunkConsumer.accept(delta);
                    }
                }
            }
            log.info(
                "Ark stream completed: modelCode={}, sessionId={}, providerRequestId={}, replyLength={}",
                request.modelCode(),
                request.sessionId(),
                providerRequestId,
                fullText.length()
            );
            return toPlainTextResponse(request, fullText.toString(), providerRequestId);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse Ark stream response: {}", ex.getMessage());
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to parse Ark stream response", HttpStatus.BAD_GATEWAY);
        } catch (IOException ex) {
            log.warn("Failed to call Ark model stream: {}", ex.getMessage());
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to call Ark model stream", HttpStatus.BAD_GATEWAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Ark model stream was interrupted");
            throw new ApiException("MODEL_PROVIDER_ERROR", "Ark model stream was interrupted", HttpStatus.BAD_GATEWAY);
        }
    }

    private ModelChatResponse parseResponse(ModelChatRequest request, String responseBody) {
        try {
            JsonNode providerPayload = objectMapper.readTree(responseBody);
            String providerRequestId = providerPayload.path("id").asText(null);
            String content = providerPayload.path("choices").path(0).path("message").path("content").asText("");
            log.info("Parsed Ark response: providerRequestId={}, contentLength={}, contentPreview={}", providerRequestId, content.length(), truncateForLog(content));
            Map<String, Object> rawPayload = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            Map<String, Object> businessPayload = fallbackPayload(content);
            businessPayload.put("rawProviderPayload", rawPayload);
            return toResponse(request, businessPayload, providerRequestId);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse Ark response: {}", ex.getMessage());
            throw new ApiException("MODEL_PROVIDER_ERROR", "Failed to parse Ark response", HttpStatus.BAD_GATEWAY);
        }
    }

    private ModelChatResponse toPlainTextResponse(ModelChatRequest request, String content, String providerRequestId) {
        return toResponse(request, fallbackPayload(content), providerRequestId);
    }

    private ModelChatResponse toResponse(ModelChatRequest request, Map<String, Object> payload, String providerRequestId) {
        return new ModelChatResponse(
            providerCode(),
            request.modelCode(),
            StringUtils.defaultIfBlank(stringValue(payload.get("replyText")), "模型没有返回有效内容，请稍后重试。"),
            intValue(payload.get("hintLevel"), 1),
            StringUtils.defaultIfBlank(stringValue(payload.get("guidanceStage")), DEFAULT_GUIDANCE_STAGE),
            StringUtils.defaultIfBlank(stringValue(payload.get("teacherIntent")), DEFAULT_TEACHER_INTENT),
            booleanValue(payload.get("shouldRevealFinalAnswer"), false),
            List.of(),
            payload,
            providerRequestId
        );
    }

    private Map<String, Object> fallbackPayload(String content) {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("replyText", StringUtils.defaultIfBlank(content, "模型没有返回有效内容，请稍后重试。").trim());
        fallback.put("guidanceStage", DEFAULT_GUIDANCE_STAGE);
        fallback.put("hintLevel", 1);
        fallback.put("teacherIntent", DEFAULT_TEACHER_INTENT);
        fallback.put("shouldRevealFinalAnswer", false);
        fallback.put("annotations", List.of());
        fallback.put("parseFallback", true);
        return fallback;
    }

    private String providerErrorMessage(String prefix, int statusCode, String responseBody) {
        String body = truncateForLog(responseBody);
        return StringUtils.isBlank(body)
            ? "%s with HTTP %d".formatted(prefix, statusCode)
            : "%s with HTTP %d: %s".formatted(prefix, statusCode, body);
    }

    private String truncateForLog(String value) {
        String normalized = StringUtils.normalizeSpace(StringUtils.defaultString(value));
        if (normalized.length() > 800) {
            return normalized.substring(0, 800) + "...";
        }
        return normalized;
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
}
