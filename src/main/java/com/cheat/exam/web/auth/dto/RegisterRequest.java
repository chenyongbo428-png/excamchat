package com.cheat.exam.web.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "username is required")
    @Size(min = 4, max = 64, message = "username length must be between 4 and 64")
    String username,
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 128, message = "password length must be between 8 and 128")
    String password,
    @Size(max = 64, message = "nickname length must be at most 64")
    String nickname
) {
}
