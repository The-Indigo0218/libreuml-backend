package com.libreuml.backend.application.emailverification.port.out;

import com.libreuml.backend.domain.model.EmailVerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {
    EmailVerificationToken save(EmailVerificationToken token);
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteAllByUserId(UUID userId);
}
