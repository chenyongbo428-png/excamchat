package com.cheat.exam.web.session.dto;

import java.time.Instant;

public record SessionSummaryResponse(
    Long sessionId,
    String title,
    String modelCode,
    Long imageId,
    Instant lastMessageAt,
    Instant createdAt
) {
}
