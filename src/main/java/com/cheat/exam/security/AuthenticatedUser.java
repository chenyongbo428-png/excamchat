package com.cheat.exam.security;

public record AuthenticatedUser(Long userId, String username, String roleCode) {
}
