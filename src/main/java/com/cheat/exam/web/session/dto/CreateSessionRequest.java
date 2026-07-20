package com.cheat.exam.web.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @NotNull(message = "imageId is required")
    Long imageId,
    @NotBlank(message = "modelCode is required")
    String modelCode,
    @Size(max = 255, message = "title length must be at most 255")
    String title,
    @Size(max = 32, message = "subjectCode length must be at most 32")
    String subjectCode,
    @Size(max = 32, message = "gradeLevel length must be at most 32")
    String gradeLevel
) {
}
