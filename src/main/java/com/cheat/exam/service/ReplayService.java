package com.cheat.exam.service;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.domain.canvas.CanvasOperation;
import com.cheat.exam.domain.message.ChatMessage;
import com.cheat.exam.domain.session.ChatSession;
import com.cheat.exam.domain.user.User;
import com.cheat.exam.repository.CanvasOperationRepository;
import com.cheat.exam.repository.ChatMessageRepository;
import com.cheat.exam.repository.ChatSessionRepository;
import com.cheat.exam.repository.UserRepository;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.web.replay.dto.ReplayBackgroundImageResponse;
import com.cheat.exam.web.replay.dto.ReplayResponse;
import com.cheat.exam.web.replay.dto.ReplaySessionResponse;
import com.cheat.exam.web.replay.dto.ReplayTimelineItemResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplayService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CanvasOperationRepository canvasOperationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ReplayService(
        ChatSessionRepository chatSessionRepository,
        ChatMessageRepository chatMessageRepository,
        CanvasOperationRepository canvasOperationRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.canvasOperationRepository = canvasOperationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ReplayResponse getReplay(AuthenticatedUser authenticatedUser, Long sessionId) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        List<ReplayEvent> events = new ArrayList<>();

        long eventOrder = 0;
        for (ChatMessage message : chatMessageRepository.findBySessionOrderByCreatedAtAsc(session)) {
            events.add(ReplayEvent.message(++eventOrder, message));
            for (Map<String, Object> annotation : readAnnotationSummary(message.getAnnotationJson())) {
                events.add(ReplayEvent.aiAnnotation(++eventOrder, message, annotation));
            }
        }
        for (CanvasOperation operation : canvasOperationRepository.findBySessionOrderBySequenceNoAsc(session)) {
            events.add(ReplayEvent.canvasOperation(++eventOrder, operation, readJsonMap(operation.getPayloadJson())));
        }

        List<ReplayTimelineItemResponse> timeline = events.stream()
            .sorted(Comparator
                .comparing(ReplayEvent::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(ReplayEvent::priority)
                .thenComparingLong(ReplayEvent::order))
            .map(new StepCounter()::toResponse)
            .toList();

        return new ReplayResponse(
            new ReplaySessionResponse(session.getId(), session.getTitle(), session.getModelCode()),
            new ReplayBackgroundImageResponse(
                session.getImage().getId(),
                "/api/images/%d/content".formatted(session.getImage().getId()),
                session.getImage().getWidth(),
                session.getImage().getHeight()
            ),
            timeline
        );
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

    private Map<String, Object> readJsonMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to parse replay payload", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private record ReplayEvent(
        long order,
        int priority,
        String stepType,
        Long messageId,
        String roleCode,
        String contentText,
        Map<String, Object> annotation,
        Map<String, Object> operation,
        Instant createdAt
    ) {
        static ReplayEvent message(long order, ChatMessage message) {
            return new ReplayEvent(
                order,
                10,
                "MESSAGE",
                message.getId(),
                message.getRoleCode(),
                message.getContentText(),
                null,
                null,
                message.getCreatedAt()
            );
        }

        static ReplayEvent aiAnnotation(long order, ChatMessage message, Map<String, Object> annotation) {
            return new ReplayEvent(
                order,
                20,
                "AI_ANNOTATION",
                message.getId(),
                message.getRoleCode(),
                null,
                annotation,
                null,
                message.getCreatedAt()
            );
        }

        static ReplayEvent canvasOperation(long order, CanvasOperation operation, Map<String, Object> payload) {
            return new ReplayEvent(
                order,
                30,
                "CANVAS_OPERATION",
                operation.getMessage() == null ? null : operation.getMessage().getId(),
                operation.getOperatorType(),
                null,
                null,
                Map.of(
                    "operationId", operation.getId(),
                    "operationType", operation.getOperationType(),
                    "layerType", operation.getLayerType(),
                    "sequenceNo", operation.getSequenceNo(),
                    "payload", payload
                ),
                operation.getCreatedAt()
            );
        }
    }

    private static final class StepCounter {
        private long stepNo;

        ReplayTimelineItemResponse toResponse(ReplayEvent event) {
            return new ReplayTimelineItemResponse(
                ++stepNo,
                event.stepType(),
                event.messageId(),
                event.roleCode(),
                event.contentText(),
                event.annotation(),
                event.operation(),
                event.createdAt()
            );
        }
    }
}
