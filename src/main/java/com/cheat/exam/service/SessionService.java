package com.cheat.exam.service;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.domain.image.ImageResource;
import com.cheat.exam.domain.message.ChatMessage;
import com.cheat.exam.domain.model.ModelConfig;
import com.cheat.exam.domain.session.ChatSession;
import com.cheat.exam.domain.user.User;
import com.cheat.exam.repository.ChatMessageRepository;
import com.cheat.exam.repository.ChatSessionRepository;
import com.cheat.exam.repository.ImageResourceRepository;
import com.cheat.exam.repository.ModelConfigRepository;
import com.cheat.exam.repository.UserRepository;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.service.ai.AiAnnotationParser;
import com.cheat.exam.service.model.ModelChatRequest;
import com.cheat.exam.service.model.ModelChatResponse;
import com.cheat.exam.service.model.ModelClient;
import com.cheat.exam.service.model.ModelClientRouter;
import com.cheat.exam.service.model.ModelClientSelection;
import com.cheat.exam.service.model.ModelImageInput;
import com.cheat.exam.service.model.ModelMessageInput;
import com.cheat.exam.web.session.dto.CreateMessageRequest;
import com.cheat.exam.web.session.dto.CreateSessionRequest;
import com.cheat.exam.web.session.dto.MessageItemResponse;
import com.cheat.exam.web.session.dto.SendMessageResponse;
import com.cheat.exam.web.session.dto.SessionDetailResponse;
import com.cheat.exam.web.session.dto.SessionImageResponse;
import com.cheat.exam.web.session.dto.SessionPageResponse;
import com.cheat.exam.web.session.dto.SessionResponse;
import com.cheat.exam.web.session.dto.SessionSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ImageResourceRepository imageResourceRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final UserRepository userRepository;
    private final CanvasService canvasService;
    private final AiAnnotationParser aiAnnotationParser;
    private final ModelClientRouter modelClientRouter;
    private final ObjectMapper objectMapper;

    public SessionService(
        ChatSessionRepository chatSessionRepository,
        ChatMessageRepository chatMessageRepository,
        ImageResourceRepository imageResourceRepository,
        ModelConfigRepository modelConfigRepository,
        UserRepository userRepository,
        CanvasService canvasService,
        AiAnnotationParser aiAnnotationParser,
        ModelClientRouter modelClientRouter,
        ObjectMapper objectMapper
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.imageResourceRepository = imageResourceRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.userRepository = userRepository;
        this.canvasService = canvasService;
        this.aiAnnotationParser = aiAnnotationParser;
        this.modelClientRouter = modelClientRouter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionResponse create(AuthenticatedUser authenticatedUser, CreateSessionRequest request) {
        User user = findCurrentUser(authenticatedUser);
        ImageResource image = imageResourceRepository.findByIdAndUser(request.imageId(), user)
            .orElseThrow(() -> new ApiException("IMAGE_NOT_FOUND", "Image not found", HttpStatus.NOT_FOUND));
        ModelConfig modelConfig = modelConfigRepository.findByModelCodeAndEnabledTrue(request.modelCode())
            .orElseThrow(() -> new ApiException("MODEL_NOT_AVAILABLE", "Model not available", HttpStatus.BAD_REQUEST));
        if (!modelConfig.isSupportsVision()) {
            throw new ApiException("MODEL_NOT_AVAILABLE", "Selected model does not support image input", HttpStatus.BAD_REQUEST);
        }

        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setImage(image);
        session.setModelCode(modelConfig.getModelCode());
        session.setTitle(StringUtils.defaultIfBlank(request.title(), "新建讲解会话"));
        session.setSubjectCode(request.subjectCode());
        session.setGradeLevel(request.gradeLevel());
        session.setStatus("ACTIVE");
        session.setLastMessageAt(Instant.now());
        session.setGuidanceStateJson(writeJson(initialGuidanceState()));
        ChatSession saved = chatSessionRepository.save(session);
        canvasService.createInitialDocument(saved);
        return new SessionResponse(
            saved.getId(),
            saved.getTitle(),
            saved.getModelCode(),
            saved.getImage().getId(),
            saved.getSubjectCode(),
            saved.getGradeLevel(),
            saved.getCreatedAt(),
            saved.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public SessionPageResponse list(AuthenticatedUser authenticatedUser, int page, int pageSize) {
        validatePage(page, pageSize);
        User user = findCurrentUser(authenticatedUser);
        Page<ChatSession> sessionPage = chatSessionRepository.findByUserAndStatus(
            user,
            "ACTIVE",
            PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        List<SessionSummaryResponse> items = sessionPage.getContent().stream()
            .map(session -> new SessionSummaryResponse(
                session.getId(),
                session.getTitle(),
                session.getModelCode(),
                session.getImage().getId(),
                session.getLastMessageAt(),
                session.getCreatedAt()
            ))
            .toList();
        return new SessionPageResponse(items, page, pageSize, sessionPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse detail(AuthenticatedUser authenticatedUser, Long sessionId) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        return new SessionDetailResponse(
            session.getId(),
            session.getTitle(),
            session.getModelCode(),
            new SessionImageResponse(
                session.getImage().getId(),
                "/api/images/%d/content".formatted(session.getImage().getId()),
                session.getImage().getWidth(),
                session.getImage().getHeight()
            ),
            session.getSubjectCode(),
            session.getGradeLevel(),
            session.getCreatedAt(),
            session.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<MessageItemResponse> listMessages(AuthenticatedUser authenticatedUser, Long sessionId) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        return chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
            .map(this::toMessageItemResponse)
            .toList();
    }

    @Transactional
    public SendMessageResponse sendMessage(AuthenticatedUser authenticatedUser, Long sessionId, CreateMessageRequest request) {
        if (Boolean.TRUE.equals(request.useStream())) {
            throw new ApiException("BAD_REQUEST", "Streaming is not supported on this endpoint", HttpStatus.BAD_REQUEST);
        }

        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRoleCode("USER");
        userMessage.setContentType("TEXT");
        userMessage.setContentText(request.content().trim());
        userMessage.setMessageStatus("SUCCESS");
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);

        ModelConfig modelConfig = modelConfigRepository.findByModelCodeAndEnabledTrue(session.getModelCode())
            .orElseThrow(() -> new ApiException("MODEL_NOT_AVAILABLE", "Model not available", HttpStatus.BAD_REQUEST));
        ModelClientSelection selection = toModelClientSelection(modelConfig);
        ModelClient modelClient = modelClientRouter.route(selection);
        ModelChatResponse modelResponse = modelClient.chat(buildModelChatRequest(
            session,
            modelConfig,
            request.content(),
            request.mode(),
            false
        ));

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRoleCode("ASSISTANT");
        assistantMessage.setContentType("TEXT");
        assistantMessage.setContentText(modelResponse.replyText());
        assistantMessage.setHintLevel(modelResponse.hintLevel());
        assistantMessage.setGuidanceStage(modelResponse.guidanceStage());
        assistantMessage.setMessageStatus("SUCCESS");
        assistantMessage.setProviderRequestId(modelResponse.providerRequestId());
        ChatMessage savedAssistantMessage = chatMessageRepository.save(assistantMessage);

        List<Map<String, Object>> normalizedAnnotations = aiAnnotationParser.normalizeAnnotations(
            session,
            savedAssistantMessage.getId(),
            modelResponse.teacherIntent(),
            modelResponse.annotations()
        );
        Map<String, Object> normalizedPayload = new LinkedHashMap<>(modelResponse.rawPayload());
        normalizedPayload.put("annotations", normalizedAnnotations);
        normalizedPayload.put("providerCode", modelResponse.providerCode());
        normalizedPayload.put("modelCode", modelResponse.modelCode());
        normalizedPayload.put("providerRequestId", modelResponse.providerRequestId());
        savedAssistantMessage.setRawPayloadJson(writeJson(normalizedPayload));
        savedAssistantMessage.setAnnotationJson(writeJson(normalizedAnnotations));
        savedAssistantMessage = chatMessageRepository.save(savedAssistantMessage);
        canvasService.applyAiAnnotations(session, savedAssistantMessage, normalizedAnnotations);
        updateGuidanceState(session, request.mode(), modelResponse);

        session.setLastMessageAt(savedAssistantMessage.getCreatedAt());
        chatSessionRepository.save(session);

        return new SendMessageResponse(
            toMessageItemResponse(savedUserMessage),
            toMessageItemResponse(savedAssistantMessage)
        );
    }

    @Transactional
    public void streamMessage(
        AuthenticatedUser authenticatedUser,
        Long sessionId,
        CreateMessageRequest request,
        OutputStream outputStream
    ) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);

        ChatMessage savedUserMessage = saveUserMessage(session, request.content());
        ModelConfig modelConfig = modelConfigRepository.findByModelCodeAndEnabledTrue(session.getModelCode())
            .orElseThrow(() -> new ApiException("MODEL_NOT_AVAILABLE", "Model not available", HttpStatus.BAD_REQUEST));
        ModelClient modelClient = modelClientRouter.route(toModelClientSelection(modelConfig));

        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try {
            writeSse(writer, "user", toMessageItemResponse(savedUserMessage));
            ModelChatResponse modelResponse = modelClient.stream(
                buildModelChatRequest(session, modelConfig, request.content(), request.mode(), true),
                chunk -> writeSseUnchecked(writer, "delta", Map.of("text", chunk))
            );

            ChatMessage savedAssistantMessage = saveAssistantMessage(session, modelResponse);
            updateGuidanceState(session, request.mode(), modelResponse);
            session.setLastMessageAt(savedAssistantMessage.getCreatedAt());
            chatSessionRepository.save(session);

            writeSse(writer, "done", new SendMessageResponse(
                toMessageItemResponse(savedUserMessage),
                toMessageItemResponse(savedAssistantMessage)
            ));
            writer.flush();
        } catch (UncheckedIOException ex) {
            throw new ApiException("STREAM_WRITE_FAILED", "Failed to write stream response", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        } catch (IOException ex) {
            throw new ApiException("STREAM_WRITE_FAILED", "Failed to write stream response", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }

    private ModelClientSelection toModelClientSelection(ModelConfig modelConfig) {
        return new ModelClientSelection(
            modelConfig.getModelCode(),
            modelConfig.getProviderCode(),
            modelConfig.isSupportsVision(),
            modelConfig.isSupportsStream(),
            modelConfig.getConfigJson()
        );
    }

    private ModelChatRequest buildModelChatRequest(
        ChatSession session,
        ModelConfig modelConfig,
        String currentUserMessage,
        String guidanceMode,
        boolean streamRequested
    ) {
        // 统一请求对象是业务层和模型适配层的边界；真实模型接入时只需要消费这个对象。
        List<ModelMessageInput> messages = chatMessageRepository.findBySessionOrderByCreatedAtAsc(session).stream()
            .map(message -> new ModelMessageInput(message.getId(), message.getRoleCode(), message.getContentText()))
            .toList();
        return new ModelChatRequest(
            session.getId(),
            modelConfig.getModelCode(),
            modelConfig.getProviderCode(),
            session.getSubjectCode(),
            session.getGradeLevel(),
            buildSystemPrompt(session, guidanceMode),
            currentUserMessage,
            new ModelImageInput(
                session.getImage().getId(),
                "/api/images/%d/content".formatted(session.getImage().getId()),
                session.getImage().getStorageKey(),
                session.getImage().getWidth(),
                session.getImage().getHeight(),
                session.getImage().getMimeType()
            ),
            messages,
            normalizeGuidanceMode(guidanceMode),
            streamRequested
        );
    }

    private ChatMessage saveUserMessage(ChatSession session, String content) {
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSession(session);
        userMessage.setRoleCode("USER");
        userMessage.setContentType("TEXT");
        userMessage.setContentText(content.trim());
        userMessage.setMessageStatus("SUCCESS");
        return chatMessageRepository.save(userMessage);
    }

    private ChatMessage saveAssistantMessage(ChatSession session, ModelChatResponse modelResponse) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSession(session);
        assistantMessage.setRoleCode("ASSISTANT");
        assistantMessage.setContentType("TEXT");
        assistantMessage.setContentText(modelResponse.replyText());
        assistantMessage.setHintLevel(modelResponse.hintLevel());
        assistantMessage.setGuidanceStage(modelResponse.guidanceStage());
        assistantMessage.setMessageStatus("SUCCESS");
        assistantMessage.setProviderRequestId(modelResponse.providerRequestId());
        ChatMessage savedAssistantMessage = chatMessageRepository.save(assistantMessage);

        List<Map<String, Object>> normalizedAnnotations = aiAnnotationParser.normalizeAnnotations(
            session,
            savedAssistantMessage.getId(),
            modelResponse.teacherIntent(),
            modelResponse.annotations()
        );
        Map<String, Object> normalizedPayload = new LinkedHashMap<>(modelResponse.rawPayload());
        normalizedPayload.put("annotations", normalizedAnnotations);
        normalizedPayload.put("providerCode", modelResponse.providerCode());
        normalizedPayload.put("modelCode", modelResponse.modelCode());
        normalizedPayload.put("providerRequestId", modelResponse.providerRequestId());
        savedAssistantMessage.setRawPayloadJson(writeJson(normalizedPayload));
        savedAssistantMessage.setAnnotationJson(writeJson(normalizedAnnotations));
        savedAssistantMessage = chatMessageRepository.save(savedAssistantMessage);
        canvasService.applyAiAnnotations(session, savedAssistantMessage, normalizedAnnotations);
        return savedAssistantMessage;
    }

    private String buildSystemPrompt(ChatSession session, String guidanceMode) {
        // 这里先放基础 Prompt 编排，后续可扩展为 PromptTemplate 表或按学科/年级动态选择。
        String subject = StringUtils.defaultIfBlank(session.getSubjectCode(), "GENERAL");
        String gradeLevel = StringUtils.defaultIfBlank(session.getGradeLevel(), "UNKNOWN");
        String mode = normalizeGuidanceMode(guidanceMode);
        if ("direct".equals(mode)) {
            return """
                你是一位准确、清晰的试题讲解老师。
                当前学科：%s；当前年级：%s。
                当前为“直答模式”：请识别题目图片，直接给出题目内容、正确答案和完整但清晰的解题过程。
                不要输出 JSON，不要输出 Markdown 代码块，不要生成画布标注协议。
                如果图片不清楚，请明确说明看不清的位置。
                """.formatted(subject, gradeLevel);
        }
        return """
            你是一位耐心、善于启发学生思考的老师。
            当前学科：%s；当前年级：%s。
            请根据题目图片和学生问题进行分步讲解，默认不要一开始直接给完整答案。
            第一轮请先识别题目、提取关键条件，并提出一个明确的小问题让学生回答。
            后续每一轮都要先判断学生回答是否接近正确，再给下一步提示；如果学生答错，请温和指出卡点。
            除非学生已经完成关键推理、连续多轮卡住或明确要求总结，否则不要直接输出完整答案。
            当前先输出自然语言文本，不强制输出 JSON；后续标注模式再恢复 annotations 协议。
            """.formatted(subject, gradeLevel) + guidanceStatePrompt(session);
    }

    private String guidanceStatePrompt(ChatSession session) {
        Map<String, Object> state = readGuidanceState(session);
        return """

            当前引导状态（仅用于控制本轮范围，不要原样展示给学生）：
            - 当前阶段：%s
            - 当前目标：%s
            - 已完成步骤：%s
            - 本题累计提示次数：%s
            - 连续未推进次数：%s
            - 上一轮学生回答判断：%s
            - 上一轮判断置信度：%s
            规则：每轮只围绕当前目标；如果无法可靠判断学生回答，明确要求学生写出具体过程，不要编造新的提示。
            当连续未推进次数达到 2 次时，先总结已确认事实并询问学生是否需要查看下一步，不要继续猜测。
            """.formatted(
            stateValue(state, "currentStage", "observe"),
            stateValue(state, "currentGoal", "识别题目中的关键条件"),
            stateValue(state, "completedSteps", "暂无"),
            stateValue(state, "hintCount", "0"),
            stateValue(state, "stuckCount", "0"),
            stateValue(state, "lastAnswerStatus", "unknown"),
            stateValue(state, "confidence", "0")
        );
    }

    private Map<String, Object> initialGuidanceState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("turnCount", 0);
        state.put("hintCount", 0);
        state.put("stuckCount", 0);
        state.put("currentStage", "observe");
        state.put("currentGoal", "识别题目中的关键条件");
        state.put("completedSteps", new ArrayList<>());
        state.put("lastAnswerStatus", "unknown");
        state.put("confidence", 0.0);
        return state;
    }

    private void updateGuidanceState(ChatSession session, String guidanceMode, ModelChatResponse response) {
        if ("direct".equalsIgnoreCase(StringUtils.trimToEmpty(guidanceMode))) {
            return;
        }
        Map<String, Object> state = readGuidanceState(session);
        int turnCount = numberValue(state.get("turnCount"), 0) + 1;
        int previousHintCount = numberValue(state.get("hintCount"), 0);
        int responseHintLevel = response.hintLevel();
        int hintCount = previousHintCount + Math.max(1, responseHintLevel);
        String stage = StringUtils.defaultIfBlank(response.guidanceStage(), "observe");
        String previousStage = StringUtils.defaultIfBlank(stringValue(state.get("currentStage")), "observe");
        String previousGoal = StringUtils.defaultIfBlank(stringValue(state.get("currentGoal")), "");
        int stuckCount = stage.equals(previousStage) ? numberValue(state.get("stuckCount"), 0) + 1 : 0;
        Map<String, Object> payload = response.rawPayload() == null ? Map.of() : response.rawPayload();
        if (!stage.equals(previousStage) && StringUtils.isNotBlank(previousGoal)) {
            List<String> completedSteps = stringListValue(state.get("completedSteps"));
            if (!completedSteps.contains(previousGoal)) {
                completedSteps.add(previousGoal);
            }
            state.put("completedSteps", completedSteps);
        }
        state.put("turnCount", turnCount);
        state.put("hintCount", hintCount);
        state.put("stuckCount", stuckCount);
        state.put("currentStage", stage);
        state.put("currentGoal", StringUtils.defaultIfBlank(
            stringValue(payload.get("nextGoal")),
            stageGoal(stage)
        ));
        state.put("lastAnswerStatus", StringUtils.defaultIfBlank(
            stringValue(payload.get("answerStatus")),
            "unknown"
        ));
        state.put("confidence", numberValue(payload.get("confidence"), 0));
        session.setGuidanceStateJson(writeJson(state));
    }

    private Map<String, Object> readGuidanceState(ChatSession session) {
        if (StringUtils.isBlank(session.getGuidanceStateJson())) {
            return initialGuidanceState();
        }
        try {
            return objectMapper.readValue(session.getGuidanceStateJson(), new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            return initialGuidanceState();
        }
    }

    private String stateValue(Map<String, Object> state, String key, String fallback) {
        Object value = state.get(key);
        if (value instanceof List<?> list) {
            return list.isEmpty() ? fallback : String.join("、", list.stream().map(String::valueOf).toList());
        }
        return StringUtils.defaultIfBlank(stringValue(value), fallback);
    }

    private List<String> stringListValue(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list.stream().map(String::valueOf).toList());
        }
        return new ArrayList<>();
    }

    private int numberValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double numberValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String stageGoal(String stage) {
        return switch (stage) {
            case "analyze" -> "确认已知条件之间的关系";
            case "solve_step" -> "完成当前一步推理";
            case "review" -> "检查推理并总结方法";
            default -> "识别题目中的关键条件";
        };
    }

    private String normalizeGuidanceMode(String guidanceMode) {
        return "direct".equalsIgnoreCase(StringUtils.trimToEmpty(guidanceMode)) ? "direct" : "guided";
    }

    private User findCurrentUser(AuthenticatedUser authenticatedUser) {
        return userRepository.findById(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException("UNAUTHORIZED", "User not found", HttpStatus.UNAUTHORIZED));
    }

    private ChatSession findOwnedActiveSession(AuthenticatedUser authenticatedUser, Long sessionId) {
        User user = findCurrentUser(authenticatedUser);
        return chatSessionRepository.findByIdAndUserAndStatus(sessionId, user, "ACTIVE")
            .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND));
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new ApiException("BAD_REQUEST", "page must be at least 1", HttpStatus.BAD_REQUEST);
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new ApiException("BAD_REQUEST", "pageSize must be between 1 and 100", HttpStatus.BAD_REQUEST);
        }
    }

    private MessageItemResponse toMessageItemResponse(ChatMessage message) {
        return new MessageItemResponse(
            message.getId(),
            message.getRoleCode(),
            message.getContentText(),
            message.getHintLevel(),
            message.getGuidanceStage(),
            extractTeacherIntent(message.getRawPayloadJson()),
            readAnnotationSummary(message.getAnnotationJson()),
            message.getCreatedAt()
        );
    }

    private String extractTeacherIntent(String rawPayloadJson) {
        if (StringUtils.isBlank(rawPayloadJson)) {
            return null;
        }
        try {
            JsonNode payload = objectMapper.readTree(rawPayloadJson);
            return payload.hasNonNull("teacherIntent") ? payload.get("teacherIntent").asText() : null;
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> readAnnotationSummary(String annotationJson) {
        if (StringUtils.isBlank(annotationJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(annotationJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to serialize message payload", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void writeSse(Writer writer, String eventName, Object data) throws IOException {
        writer.write("event: ");
        writer.write(eventName);
        writer.write("\n");
        writer.write("data: ");
        writer.write(objectMapper.writeValueAsString(data));
        writer.write("\n\n");
        writer.flush();
    }

    private void writeSseUnchecked(Writer writer, String eventName, Object data) {
        try {
            writeSse(writer, eventName, data);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

}
