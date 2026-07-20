package com.cheat.exam.web.model.dto;

public record ModelItemResponse(
    String modelCode,
    String displayName,
    String providerCode,
    boolean supportsVision,
    boolean supportsStream
) {
}
