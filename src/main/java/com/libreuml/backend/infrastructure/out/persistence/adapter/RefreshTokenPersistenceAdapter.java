package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.auth.port.out.RefreshTokenRepository;
import com.libreuml.backend.domain.model.RefreshToken;
import com.libreuml.backend.infrastructure.out.persistence.entity.RefreshTokenEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity saved = springRepository.save(toEntity(token));
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByIdAndUserId(UUID id, UUID userId) {
        return springRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public List<RefreshToken> findAllActiveByUserId(UUID userId) {
        return springRepository.findAllByUserIdAndRevokedFalseAndExpiresAtAfter(userId, Instant.now())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springRepository.deleteById(id);
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        springRepository.deleteAllByUserId(userId);
    }

    private RefreshTokenEntity toEntity(RefreshToken domain) {
        return RefreshTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .issuedAt(domain.getIssuedAt())
                .expiresAt(domain.getExpiresAt())
                .revoked(domain.isRevoked())
                .userAgent(domain.getUserAgent())
                .ipAddress(domain.getIpAddress())
                .build();
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .issuedAt(entity.getIssuedAt())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.isRevoked())
                .userAgent(entity.getUserAgent())
                .ipAddress(entity.getIpAddress())
                .build();
    }
}
