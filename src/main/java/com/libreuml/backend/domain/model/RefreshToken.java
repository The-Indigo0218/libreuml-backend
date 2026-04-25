package com.libreuml.backend.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RefreshToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked;
    private String userAgent;
    private String ipAddress;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public RefreshToken revoke() {
        return RefreshToken.builder()
                .id(this.id)
                .userId(this.userId)
                .tokenHash(this.tokenHash)
                .issuedAt(this.issuedAt)
                .expiresAt(this.expiresAt)
                .revoked(true)
                .ipAddress(this.ipAddress)
                .userAgent(this.userAgent)
                .build();
    }
}
