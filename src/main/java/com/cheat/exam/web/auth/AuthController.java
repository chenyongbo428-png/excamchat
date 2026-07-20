package com.cheat.exam.web.auth;

import com.cheat.exam.common.api.ApiResponse;
import com.cheat.exam.security.SecurityUtils;
import com.cheat.exam.service.AuthService;
import com.cheat.exam.web.auth.dto.LoginRequest;
import com.cheat.exam.web.auth.dto.LoginResponse;
import com.cheat.exam.web.auth.dto.RegisterRequest;
import com.cheat.exam.web.auth.dto.RegisterResponse;
import com.cheat.exam.web.auth.dto.UserProfileResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout() {
        return ApiResponse.ok(Boolean.TRUE);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.currentUser()));
    }
}
