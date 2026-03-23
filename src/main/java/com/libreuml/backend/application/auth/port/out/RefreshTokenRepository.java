package com.libreuml.backend.application.auth.port.out;

import com.libreuml.backend.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteById(UUID id);
    void deleteAllByUserId(UUID userId);
}
