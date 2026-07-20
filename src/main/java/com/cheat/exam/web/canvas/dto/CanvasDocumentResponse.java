package com.cheat.exam.web.canvas.dto;

import java.time.Instant;
import java.util.Map;

public record CanvasDocumentResponse(
    Long sessionId,
    CanvasBackgroundImageResponse backgroundImage,
    int version,
    Map<String, Object> snapshot,
    Instant updatedAt
) {
}
