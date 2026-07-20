package com.cheat.exam.web.session.dto;

import java.time.Instant;

public record SessionResponse(
    Long sessionId,
    String title,
    String modelCode,
    Long imageId,
    String subjectCode,
    String gradeLevel,
    Instant createdAt,
    Instant updatedAt
) {
}
