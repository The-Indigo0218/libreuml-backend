package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    Optional<RefreshTokenEntity> findByIdAndUserId(UUID id, UUID userId);
    List<RefreshTokenEntity> findAllByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, Instant now);
    void deleteAllByUserId(UUID userId);
}
