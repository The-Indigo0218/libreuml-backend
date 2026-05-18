package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.emailverification.port.out.EmailVerificationTokenRepository;
import com.libreuml.backend.domain.model.EmailVerificationToken;
import com.libreuml.backend.infrastructure.out.persistence.entity.EmailVerificationTokenEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataEmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenPersistenceAdapter implements EmailVerificationTokenRepository {

    private final SpringDataEmailVerificationTokenRepository jpaRepository;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return toDomain(jpaRepository.save(toEntity(token)));
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        jpaRepository.deleteAllByUserId(userId);
    }

    private EmailVerificationTokenEntity toEntity(EmailVerificationToken domain) {
        return EmailVerificationTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .build();
    }

    private EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        return EmailVerificationToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
