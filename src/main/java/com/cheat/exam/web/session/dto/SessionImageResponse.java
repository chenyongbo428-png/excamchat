package com.cheat.exam.web.session.dto;

public record SessionImageResponse(
    Long imageId,
    String accessUrl,
    Integer width,
    Integer height
) {
}
