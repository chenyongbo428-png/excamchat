package com.cheat.exam.web.replay.dto;

import java.time.Instant;
import java.util.Map;

public record ReplayTimelineItemResponse(
    long stepNo,
    String stepType,
    Long messageId,
    String roleCode,
    String contentText,
    Map<String, Object> annotation,
    Map<String, Object> operation,
    Instant createdAt
) {
}
