package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.apikey.port.out.ApiKeyRepository;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.ApiKey;
import com.libreuml.backend.infrastructure.out.persistence.entity.ApiKeyEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApiKeyPersistenceAdapter implements ApiKeyRepository {

    private final SpringDataApiKeyRepository jpaRepository;

    @Override
    public ApiKey save(ApiKey apiKey) {
        return toDomain(jpaRepository.save(toEntity(apiKey)));
    }

    @Override
    public Optional<ApiKey> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ApiKey> findByHashedKey(String hashedKey) {
        return jpaRepository.findByHashedKey(hashedKey).map(this::toDomain);
    }

    @Override
    public List<ApiKey> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public PagedResult<ApiKey> findAllPartnerKeys(int page, int size) {
        Page<ApiKeyEntity> result = jpaRepository.findAllByKeyType(
                ApiKey.KeyType.PARTNER,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PagedResult<>(
                result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast());
    }

    @Override
    public Optional<ApiKey> findByRedemptionCode(String redemptionCode) {
        return jpaRepository.findByRedemptionCode(redemptionCode).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    /**
     * Atomic usage counter increment. Annotated with {@code @Transactional} so Spring
     * creates a fresh transaction when this method is called from a
     * {@link java.util.concurrent.CompletableFuture} thread in the authentication filter.
     */
    @Override
    @Transactional
    public void recordUsage(UUID keyId) {
        jpaRepository.recordUsage(keyId, Instant.now());
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private ApiKeyEntity toEntity(ApiKey domain) {
        return ApiKeyEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .name(domain.getName())
                .keyPrefix(domain.getKeyPrefix())
                .hashedKey(domain.getHashedKey())
                .keyType(domain.getKeyType())
                .scope(domain.getScope())
                .partnerName(domain.getPartnerName())
                .partnerEmail(domain.getPartnerEmail())
                .rateLimitRead(domain.getRateLimitRead())
                .rateLimitWrite(domain.getRateLimitWrite())
                .redemptionCode(domain.getRedemptionCode())
                .redemptionLimit(domain.getRedemptionLimit())
                .redemptionCount(domain.getRedemptionCount())
                .createdAt(domain.getCreatedAt())
                .createdBy(domain.getCreatedBy())
                .lastUsedAt(domain.getLastUsedAt())
                .usageCount(domain.getUsageCount())
                .active(domain.isActive())
                .revokedAt(domain.getRevokedAt())
                .revokedBy(domain.getRevokedBy())
                .notes(domain.getNotes())
                .build();
    }

    private ApiKey toDomain(ApiKeyEntity entity) {
        return ApiKey.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .keyPrefix(entity.getKeyPrefix())
                .hashedKey(entity.getHashedKey())
                .keyType(entity.getKeyType())
                .scope(entity.getScope())
                .partnerName(entity.getPartnerName())
                .partnerEmail(entity.getPartnerEmail())
                .rateLimitRead(entity.getRateLimitRead())
                .rateLimitWrite(entity.getRateLimitWrite())
                .redemptionCode(entity.getRedemptionCode())
                .redemptionLimit(entity.getRedemptionLimit())
                .redemptionCount(entity.getRedemptionCount())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .lastUsedAt(entity.getLastUsedAt())
                .usageCount(entity.getUsageCount())
                .active(entity.isActive())
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy())
                .notes(entity.getNotes())
                .build();
    }
}
