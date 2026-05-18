package com.libreuml.backend.application.passwordreset.port.out;

import com.libreuml.backend.domain.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteAllByUserId(UUID userId);
}
