package com.cheat.exam.web.canvas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CanvasOperationRequest(
    @NotBlank(message = "operationType is required")
    String operationType,
    @NotBlank(message = "layerType is required")
    String layerType,
    Long messageId,
    @NotNull(message = "payload is required")
    Map<String, Object> payload
) {
}
