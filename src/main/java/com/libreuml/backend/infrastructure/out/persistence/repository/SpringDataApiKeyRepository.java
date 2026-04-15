package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.domain.model.ApiKey;
import com.libreuml.backend.infrastructure.out.persistence.entity.ApiKeyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataApiKeyRepository extends JpaRepository<ApiKeyEntity, UUID> {

    Optional<ApiKeyEntity> findByHashedKey(String hashedKey);

    List<ApiKeyEntity> findAllByUserId(UUID userId);

    Page<ApiKeyEntity> findAllByKeyType(ApiKey.KeyType keyType, Pageable pageable);

    Optional<ApiKeyEntity> findByRedemptionCode(String redemptionCode);

    /**
     * Atomic in-place update for usage tracking. Runs outside the caller's transaction
     * context when invoked asynchronously from the authentication filter.
     */
    @Modifying
    @Query("UPDATE ApiKeyEntity k SET k.usageCount = k.usageCount + 1, k.lastUsedAt = :now WHERE k.id = :id")
    void recordUsage(@Param("id") UUID id, @Param("now") Instant now);
}
