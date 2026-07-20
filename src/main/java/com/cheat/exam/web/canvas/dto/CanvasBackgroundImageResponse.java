package com.cheat.exam.web.canvas.dto;

public record CanvasBackgroundImageResponse(
    Long imageId,
    String accessUrl,
    Integer width,
    Integer height
) {
}
