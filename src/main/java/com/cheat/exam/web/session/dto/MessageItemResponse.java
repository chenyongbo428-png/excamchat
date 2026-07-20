package com.cheat.exam.web.session.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageItemResponse(
    Long messageId,
    String roleCode,
    String contentText,
    Integer hintLevel,
    String guidanceStage,
    String teacherIntent,
    List<Map<String, Object>> annotationSummary,
    Instant createdAt
) {
}
