package com.cheat.exam.web.replay.dto;

public record ReplaySessionResponse(
    Long sessionId,
    String title,
    String modelCode
) {
}
