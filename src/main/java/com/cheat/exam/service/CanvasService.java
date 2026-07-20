package com.cheat.exam.service;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.domain.canvas.CanvasDocument;
import com.cheat.exam.domain.canvas.CanvasOperation;
import com.cheat.exam.domain.message.ChatMessage;
import com.cheat.exam.domain.session.ChatSession;
import com.cheat.exam.domain.user.User;
import com.cheat.exam.repository.CanvasDocumentRepository;
import com.cheat.exam.repository.CanvasOperationRepository;
import com.cheat.exam.repository.ChatMessageRepository;
import com.cheat.exam.repository.ChatSessionRepository;
import com.cheat.exam.repository.UserRepository;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.web.canvas.dto.AppendCanvasOperationsRequest;
import com.cheat.exam.web.canvas.dto.AppendCanvasOperationsResponse;
import com.cheat.exam.web.canvas.dto.CanvasBackgroundImageResponse;
import com.cheat.exam.web.canvas.dto.CanvasDocumentResponse;
import com.cheat.exam.web.canvas.dto.CanvasOperationRequest;
import com.cheat.exam.web.canvas.dto.SaveCanvasRequest;
import com.cheat.exam.web.canvas.dto.SaveCanvasResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanvasService {

    private static final List<String> ALLOWED_OPERATION_TYPES = List.of(
        "ADD_OBJECT",
        "UPDATE_OBJECT",
        "DELETE_OBJECT",
        "CLEAR_LAYER",
        "SET_LAYER_VISIBLE"
    );
    private static final List<String> ALLOWED_LAYER_TYPES = List.of("AI", "USER");

    private final CanvasDocumentRepository canvasDocumentRepository;
    private final CanvasOperationRepository canvasOperationRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CanvasService(
        CanvasDocumentRepository canvasDocumentRepository,
        CanvasOperationRepository canvasOperationRepository,
        ChatSessionRepository chatSessionRepository,
        ChatMessageRepository chatMessageRepository,
        UserRepository userRepository,
        ObjectMapper objectMapper
    ) {
        this.canvasDocumentRepository = canvasDocumentRepository;
        this.canvasOperationRepository = canvasOperationRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CanvasDocument createInitialDocument(ChatSession session) {
        CanvasDocument document = new CanvasDocument();
        document.setSession(session);
        document.setBackgroundImage(session.getImage());
        document.setSnapshotJson(writeJson(buildInitialSnapshot(session)));
        document.setVersionNo(1);
        document.setUpdatedByType("SYSTEM");
        return canvasDocumentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public CanvasDocumentResponse getDocument(AuthenticatedUser authenticatedUser, Long sessionId) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        CanvasDocument document = canvasDocumentRepository.findBySession(session)
            .orElseThrow(() -> new ApiException("CANVAS_NOT_FOUND", "Canvas document not found", HttpStatus.NOT_FOUND));
        return toDocumentResponse(document);
    }

    @Transactional
    public SaveCanvasResponse saveDocument(AuthenticatedUser authenticatedUser, Long sessionId, SaveCanvasRequest request) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        CanvasDocument document = canvasDocumentRepository.findBySession(session)
            .orElseGet(() -> createInitialDocument(session));
        if (request.version() != null && request.version() != document.getVersionNo()) {
            throw new ApiException("CANVAS_VERSION_CONFLICT", "Canvas version has changed", HttpStatus.CONFLICT);
        }
        validateSnapshot(objectMapper.valueToTree(request.snapshot()), session);
        document.setSnapshotJson(writeJson(request.snapshot()));
        document.setVersionNo(document.getVersionNo() + 1);
        document.setUpdatedByType("USER");
        document.setUpdatedById(authenticatedUser.userId());
        CanvasDocument saved = canvasDocumentRepository.save(document);
        return new SaveCanvasResponse(session.getId(), saved.getVersionNo(), saved.getUpdatedAt());
    }

    @Transactional
    public AppendCanvasOperationsResponse appendOperations(
        AuthenticatedUser authenticatedUser,
        Long sessionId,
        AppendCanvasOperationsRequest request
    ) {
        ChatSession session = findOwnedActiveSession(authenticatedUser, sessionId);
        CanvasDocument document = canvasDocumentRepository.findBySession(session)
            .orElseGet(() -> createInitialDocument(session));

        long sequenceNo = canvasOperationRepository.findMaxSequenceNoBySession(session);
        for (CanvasOperationRequest operationRequest : request.operations()) {
            validateOperation(operationRequest);
            CanvasOperation operation = new CanvasOperation();
            operation.setSession(session);
            operation.setMessage(resolveMessage(session, operationRequest.messageId()));
            operation.setOperatorType("USER");
            operation.setOperatorId(authenticatedUser.userId());
            operation.setOperationType(operationRequest.operationType());
            operation.setLayerType(operationRequest.layerType());
            operation.setPayloadJson(writeJson(operationRequest.payload()));
            operation.setSequenceNo(++sequenceNo);
            canvasOperationRepository.save(operation);
        }

        document.setVersionNo(document.getVersionNo() + request.operations().size());
        document.setUpdatedByType("USER");
        document.setUpdatedById(authenticatedUser.userId());
        CanvasDocument saved = canvasDocumentRepository.save(document);
        return new AppendCanvasOperationsResponse(request.operations().size(), saved.getVersionNo());
    }

    @Transactional
    public void applyAiAnnotations(ChatSession session, ChatMessage message, List<Map<String, Object>> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        CanvasDocument document = canvasDocumentRepository.findBySession(session)
            .orElseGet(() -> createInitialDocument(session));
        Map<String, Object> snapshot = readJsonMap(document.getSnapshotJson());
        Map<String, Object> aiLayer = findOrCreateLayer(snapshot, "ai-layer", "AI", true);
        List<Map<String, Object>> objects = readLayerObjects(aiLayer);
        objects.addAll(annotations);
        aiLayer.put("objects", objects);

        document.setSnapshotJson(writeJson(snapshot));
        document.setVersionNo(document.getVersionNo() + 1);
        document.setUpdatedByType("AI");
        document.setUpdatedById(message.getId());
        canvasDocumentRepository.save(document);
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

    private ChatMessage resolveMessage(ChatSession session, Long messageId) {
        if (messageId == null) {
            return null;
        }
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new ApiException("MESSAGE_NOT_FOUND", "Message not found", HttpStatus.NOT_FOUND));
        if (!message.getSession().getId().equals(session.getId())) {
            throw new ApiException("MESSAGE_NOT_FOUND", "Message not found", HttpStatus.NOT_FOUND);
        }
        return message;
    }

    private CanvasDocumentResponse toDocumentResponse(CanvasDocument document) {
        return new CanvasDocumentResponse(
            document.getSession().getId(),
            new CanvasBackgroundImageResponse(
                document.getBackgroundImage().getId(),
                "/api/images/%d/content".formatted(document.getBackgroundImage().getId()),
                document.getBackgroundImage().getWidth(),
                document.getBackgroundImage().getHeight()
            ),
            document.getVersionNo(),
            readJsonMap(document.getSnapshotJson()),
            document.getUpdatedAt()
        );
    }

    private void validateSnapshot(JsonNode snapshot, ChatSession session) {
        if (snapshot == null || !snapshot.isObject()) {
            throw new ApiException("BAD_REQUEST", "snapshot must be an object", HttpStatus.BAD_REQUEST);
        }
        JsonNode schemaVersion = snapshot.get("schemaVersion");
        if (schemaVersion == null || StringUtils.isBlank(schemaVersion.asText())) {
            throw new ApiException("BAD_REQUEST", "snapshot.schemaVersion is required", HttpStatus.BAD_REQUEST);
        }
        JsonNode background = snapshot.get("background");
        if (background == null || !background.isObject() || background.get("imageId") == null) {
            throw new ApiException("BAD_REQUEST", "snapshot.background.imageId is required", HttpStatus.BAD_REQUEST);
        }
        if (!String.valueOf(session.getImage().getId()).equals(background.get("imageId").asText())) {
            throw new ApiException("BAD_REQUEST", "snapshot background image does not match session image", HttpStatus.BAD_REQUEST);
        }
        JsonNode layers = snapshot.get("layers");
        if (layers == null || !layers.isArray() || layers.isEmpty()) {
            throw new ApiException("BAD_REQUEST", "snapshot.layers must not be empty", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateOperation(CanvasOperationRequest operationRequest) {
        if (!ALLOWED_OPERATION_TYPES.contains(operationRequest.operationType())) {
            throw new ApiException("BAD_REQUEST", "Unsupported canvas operation type", HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_LAYER_TYPES.contains(operationRequest.layerType())) {
            throw new ApiException("BAD_REQUEST", "Unsupported canvas layer type", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> buildInitialSnapshot(ChatSession session) {
        return Map.of(
            "schemaVersion", "1.0",
            "background", Map.of(
                "imageId", String.valueOf(session.getImage().getId()),
                "width", session.getImage().getWidth() == null ? 0 : session.getImage().getWidth(),
                "height", session.getImage().getHeight() == null ? 0 : session.getImage().getHeight()
            ),
            "viewport", Map.of(
                "zoom", 1,
                "panX", 0,
                "panY", 0
            ),
            "layers", List.of(
                Map.of("layerId", "ai-layer", "layerType", "AI", "visible", true, "locked", false, "objects", List.of()),
                Map.of("layerId", "user-layer", "layerType", "USER", "visible", true, "locked", false, "objects", List.of())
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findOrCreateLayer(
        Map<String, Object> snapshot,
        String layerId,
        String layerType,
        boolean locked
    ) {
        Object rawLayers = snapshot.get("layers");
        List<Map<String, Object>> layers = rawLayers instanceof List<?> list
            ? new ArrayList<>((List<Map<String, Object>>) list)
            : new ArrayList<>();
        for (Map<String, Object> layer : layers) {
            if (layerType.equals(layer.get("layerType"))) {
                snapshot.put("layers", layers);
                return layer;
            }
        }
        Map<String, Object> layer = new LinkedHashMap<>();
        layer.put("layerId", layerId);
        layer.put("layerType", layerType);
        layer.put("visible", true);
        layer.put("locked", locked);
        layer.put("objects", new ArrayList<Map<String, Object>>());
        layers.add(layer);
        snapshot.put("layers", layers);
        return layer;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readLayerObjects(Map<String, Object> layer) {
        Object rawObjects = layer.get("objects");
        if (rawObjects instanceof List<?> list) {
            return new ArrayList<>((List<Map<String, Object>>) list);
        }
        return new ArrayList<>();
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to parse canvas snapshot", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> readJsonMap(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to parse canvas snapshot", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ApiException("INTERNAL_ERROR", "Failed to serialize canvas data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
