package com.cheat.exam.web.image.dto;

import java.time.Instant;

public record ImageResponse(
    Long imageId,
    String fileName,
    String mimeType,
    long fileSize,
    Integer width,
    Integer height,
    String accessUrl,
    Instant createdAt
) {
}
