package com.cheat.exam.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "username is required")
    String username,
    @NotBlank(message = "password is required")
    String password
) {
}
