package com.cheat.exam.service;

import com.cheat.exam.common.api.ApiException;
import com.cheat.exam.domain.user.User;
import com.cheat.exam.repository.UserRepository;
import com.cheat.exam.web.auth.dto.LoginRequest;
import com.cheat.exam.web.auth.dto.LoginResponse;
import com.cheat.exam.web.auth.dto.RegisterRequest;
import com.cheat.exam.web.auth.dto.RegisterResponse;
import com.cheat.exam.web.auth.dto.UserProfileResponse;
import com.cheat.exam.security.AuthenticatedUser;
import com.cheat.exam.security.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException("BAD_REQUEST", "Username already exists", HttpStatus.BAD_REQUEST);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.defaultIfBlank(request.nickname(), request.username()));
        user.setRoleCode("USER");
        user.setStatus("ACTIVE");
        User saved = userRepository.save(user);
        return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getNickname());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ApiException("UNAUTHORIZED", "Invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException("UNAUTHORIZED", "Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException("FORBIDDEN", "User is disabled", HttpStatus.FORBIDDEN);
        }
        String token = tokenService.createToken(user);
        return new LoginResponse(
            token,
            "Bearer",
            7200,
            new UserProfileResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(), user.getRoleCode())
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse me(AuthenticatedUser authenticatedUser) {
        User user = userRepository.findById(authenticatedUser.userId())
            .orElseThrow(() -> new ApiException("UNAUTHORIZED", "User not found", HttpStatus.UNAUTHORIZED));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(), user.getRoleCode());
    }
}
