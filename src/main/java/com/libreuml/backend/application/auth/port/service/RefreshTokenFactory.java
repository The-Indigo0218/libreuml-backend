package com.libreuml.backend.application.auth.port.service;

import com.libreuml.backend.application.auth.dto.IssuedRefreshToken;
import com.libreuml.backend.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Single source of truth for refresh-token creation and hashing, shared by every flow that
 * issues sessions (credential login, refresh rotation and OAuth login). Centralizing this
 * removes the previously duplicated {@code generateOpaqueToken}/{@code sha256Hex} logic and
 * keeps the validity window and hashing scheme consistent across all of them.
 */
@Component
public class RefreshTokenFactory {

    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 7;
    private static final int RAW_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new opaque refresh token for the user and builds its persistable record.
     * The returned raw value is never stored — only its hash is.
     */
    public IssuedRefreshToken issue(UUID userId, String ipAddress, String userAgent) {
        String rawToken = generateOpaqueToken();
        Instant now = Instant.now();

        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(hash(rawToken))
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return new IssuedRefreshToken(rawToken, token);
    }

    /** SHA-256 hash (hex) used to look up and revoke a refresh token by its raw value. */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
