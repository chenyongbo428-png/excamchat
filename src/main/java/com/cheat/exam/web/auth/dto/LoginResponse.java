package com.cheat.exam.web.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserProfileResponse user
) {
}
