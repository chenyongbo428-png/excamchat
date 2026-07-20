package com.cheat.exam.web.replay.dto;

public record ReplayBackgroundImageResponse(
    Long imageId,
    String accessUrl,
    Integer width,
    Integer height
) {
}
