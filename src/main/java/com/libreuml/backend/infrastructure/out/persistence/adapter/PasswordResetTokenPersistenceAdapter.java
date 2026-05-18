package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.passwordreset.port.out.PasswordResetTokenRepository;
import com.libreuml.backend.domain.model.PasswordResetToken;
import com.libreuml.backend.infrastructure.out.persistence.entity.PasswordResetTokenEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataPasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository jpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return toDomain(jpaRepository.save(toEntity(token)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        jpaRepository.deleteAllByUserId(userId);
    }

    private PasswordResetTokenEntity toEntity(PasswordResetToken domain) {
        return PasswordResetTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .build();
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
