package com.cheat.exam.web.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
    @NotBlank(message = "content is required")
    @Size(max = 4000, message = "content length must be at most 4000")
    String content,
    Boolean useStream,
    @Size(max = 16, message = "mode length must be at most 16")
    String mode
) {
}
