package com.libreuml.backend.application.apikey.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.ApiKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {

    ApiKey save(ApiKey apiKey);

    Optional<ApiKey> findById(UUID id);

    Optional<ApiKey> findByHashedKey(String hashedKey);

    List<ApiKey> findAllByUserId(UUID userId);

    PagedResult<ApiKey> findAllPartnerKeys(int page, int size);

    Optional<ApiKey> findByRedemptionCode(String redemptionCode);

    void deleteById(UUID id);

    /**
     * Atomically increments {@code usage_count} and updates {@code last_used_at} to NOW().
     * Designed to be called asynchronously from the authentication filter; must not block
     * the calling thread's transaction.
     */
    void recordUsage(UUID keyId);
}
