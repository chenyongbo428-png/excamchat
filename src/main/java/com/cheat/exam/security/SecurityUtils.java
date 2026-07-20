package com.cheat.exam.security;

import com.cheat.exam.common.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException("UNAUTHORIZED", "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }
}
