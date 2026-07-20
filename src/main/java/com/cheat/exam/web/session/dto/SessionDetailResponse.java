package com.cheat.exam.web.session.dto;

import java.time.Instant;

public record SessionDetailResponse(
    Long sessionId,
    String title,
    String modelCode,
    SessionImageResponse image,
    String subjectCode,
    String gradeLevel,
    Instant createdAt,
    Instant updatedAt
) {
}
