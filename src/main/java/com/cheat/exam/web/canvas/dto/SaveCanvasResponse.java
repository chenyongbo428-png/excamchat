package com.cheat.exam.web.canvas.dto;

import java.time.Instant;

public record SaveCanvasResponse(
    Long sessionId,
    int version,
    Instant updatedAt
) {
}
