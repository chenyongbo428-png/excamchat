package com.cheat.exam.web.canvas.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SaveCanvasRequest(
    Integer version,
    @NotNull(message = "snapshot is required")
    Map<String, Object> snapshot
) {
}
