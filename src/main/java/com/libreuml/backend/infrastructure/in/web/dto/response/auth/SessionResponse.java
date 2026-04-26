package com.libreuml.backend.infrastructure.in.web.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.libreuml.backend.domain.model.RefreshToken;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(
        UUID id,
        Instant issuedAt,
        Instant expiresAt,
        String ipAddress,
        String userAgent
) {
    public static SessionResponse from(RefreshToken token) {
        return new SessionResponse(
                token.getId(),
                token.getIssuedAt(),
                token.getExpiresAt(),
                token.getIpAddress(),
                token.getUserAgent()
        );
    }
}
