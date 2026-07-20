package com.cheat.exam.security;

import com.cheat.exam.config.AppProperties;
import com.cheat.exam.domain.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final AppProperties appProperties;

    public TokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createToken(User user) {
        long expiresAt = Instant.now().plusSeconds(appProperties.getSecurity().getTokenExpirationSeconds()).getEpochSecond();
        String raw = user.getId() + ":" + user.getUsername() + ":" + user.getRoleCode() + ":" + expiresAt;
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<AuthenticatedUser> parseToken(String token) {
        if (StringUtils.isBlank(token)) {
            return Optional.empty();
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(parts[3]);
            if (Instant.now().getEpochSecond() > expiresAt) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(Long.parseLong(parts[0]), parts[1], parts[2]));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
