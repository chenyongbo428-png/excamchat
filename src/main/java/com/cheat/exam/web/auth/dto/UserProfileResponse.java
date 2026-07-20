package com.cheat.exam.web.auth.dto;

public record UserProfileResponse(Long userId, String username, String nickname, String avatarUrl, String roleCode) {
}
