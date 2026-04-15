package com.libreuml.backend.application.apikey.port.service;

import com.libreuml.backend.application.apikey.dto.*;
import com.libreuml.backend.application.apikey.exception.*;
import com.libreuml.backend.application.apikey.port.in.*;
import com.libreuml.backend.application.apikey.port.out.ApiKeyRepository;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService implements
        CreateApiKeyUseCase,
        ListUserApiKeysUseCase,
        RevokeApiKeyUseCase,
        CreatePartnerKeyUseCase,
        ListPartnerKeysUseCase,
        GetPartnerKeyUseCase,
        UpdatePartnerKeyLimitsUseCase,
        UpdatePartnerKeyStatusUseCase,
        RevokePartnerKeyUseCase,
        RedeemPartnerCodeUseCase {

    private final ApiKeyRepository apiKeyRepository;

    // ── USER KEY OPERATIONS ──────────────────────────────────────────────────

    @Override
    public CreatedApiKeyResult createUserKey(CreateApiKeyCommand command) {
        String plainKey  = generateKey(ApiKey.KeyType.USER);
        String hashedKey = hashKey(plainKey);
        String prefix    = plainKey.substring(0, Math.min(15, plainKey.length()));

        ApiKey apiKey = ApiKey.builder()
                .userId(command.userId())
                .name(command.name())
                .keyPrefix(prefix)
                .hashedKey(hashedKey)
                .keyType(ApiKey.KeyType.USER)
                .scope(command.scope())
                .createdAt(Instant.now())
                .createdBy(command.userId())
                .build();

        return new CreatedApiKeyResult(apiKeyRepository.save(apiKey), plainKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> listByUser(UUID userId) {
        return apiKeyRepository.findAllByUserId(userId);
    }

    @Override
    public void revoke(UUID keyId, UUID requesterId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ApiKeyNotFoundException("API key not found: " + keyId));

        if (!requesterId.equals(key.getUserId())) {
            throw new ApiKeyOwnershipException("You do not own API key: " + keyId);
        }

        key.revoke(requesterId);
        apiKeyRepository.save(key);
    }

    // ── PARTNER KEY OPERATIONS ───────────────────────────────────────────────

    @Override
    public CreatedApiKeyResult createPartnerKey(CreatePartnerKeyCommand command) {
        String plainKey  = generateKey(ApiKey.KeyType.PARTNER);
        String hashedKey = hashKey(plainKey);
        String prefix    = plainKey.substring(0, Math.min(15, plainKey.length()));

        ApiKey apiKey = ApiKey.builder()
                .name(command.name())
                .keyPrefix(prefix)
                .hashedKey(hashedKey)
                .keyType(ApiKey.KeyType.PARTNER)
                .scope(command.scope())
                .partnerName(command.partnerName())
                .partnerEmail(command.partnerEmail())
                .rateLimitRead(command.rateLimitRead())
                .rateLimitWrite(command.rateLimitWrite())
                .redemptionCode(command.redemptionCode())
                .redemptionLimit(command.redemptionLimit())
                .notes(command.notes())
                .createdAt(Instant.now())
                .createdBy(command.adminId())
                .build();

        return new CreatedApiKeyResult(apiKeyRepository.save(apiKey), plainKey);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ApiKey> listPartnerKeys(int page, int size) {
        return apiKeyRepository.findAllPartnerKeys(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKey getPartnerKey(UUID id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new ApiKeyNotFoundException("Partner key not found: " + id));
    }

    @Override
    public ApiKey updateLimits(UpdatePartnerKeyLimitsCommand command) {
        ApiKey key = apiKeyRepository.findById(command.keyId())
                .orElseThrow(() -> new ApiKeyNotFoundException("Partner key not found: " + command.keyId()));

        key.setRateLimitRead(command.rateLimitRead());
        key.setRateLimitWrite(command.rateLimitWrite());
        return apiKeyRepository.save(key);
    }

    @Override
    public ApiKey updateStatus(UpdatePartnerKeyStatusCommand command) {
        ApiKey key = apiKeyRepository.findById(command.keyId())
                .orElseThrow(() -> new ApiKeyNotFoundException("Partner key not found: " + command.keyId()));

        key.setActive(command.active());
        // Clear revocation fields if re-activating.
        if (command.active()) {
            key.setRevokedAt(null);
            key.setRevokedBy(null);
        }
        return apiKeyRepository.save(key);
    }

    @Override
    public void revokePartnerKey(UUID keyId, UUID adminId) {
        ApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ApiKeyNotFoundException("Partner key not found: " + keyId));

        key.revoke(adminId);
        apiKeyRepository.save(key);
    }

    // ── REDEMPTION ───────────────────────────────────────────────────────────

    @Override
    public CreatedApiKeyResult redeem(String redemptionCode, UUID userId) {
        ApiKey partnerKey = apiKeyRepository.findByRedemptionCode(redemptionCode)
                .orElseThrow(() -> new InvalidRedemptionCodeException(
                        "Redemption code not found or inactive: " + redemptionCode));

        if (!partnerKey.isActive()) {
            throw new InvalidRedemptionCodeException(
                    "Redemption code is no longer active: " + redemptionCode);
        }

        if (!partnerKey.canBeRedeemed()) {
            throw new RedemptionLimitExceededException(
                    "Redemption limit reached for code: " + redemptionCode);
        }

        // Create a new USER key inheriting the partner's rate limits and scope.
        String plainKey  = generateKey(ApiKey.KeyType.USER);
        String hashedKey = hashKey(plainKey);
        String prefix    = plainKey.substring(0, Math.min(15, plainKey.length()));

        ApiKey userKey = ApiKey.builder()
                .userId(userId)
                .name("Redeemed: " + partnerKey.getName())
                .keyPrefix(prefix)
                .hashedKey(hashedKey)
                .keyType(ApiKey.KeyType.USER)
                .scope(partnerKey.getScope())
                .rateLimitRead(partnerKey.getRateLimitRead())
                .rateLimitWrite(partnerKey.getRateLimitWrite())
                .createdAt(Instant.now())
                .createdBy(userId)
                .build();

        ApiKey saved = apiKeyRepository.save(userKey);

        // Track redemption usage on the partner key template.
        partnerKey.incrementRedemptionCount();
        apiKeyRepository.save(partnerKey);

        return new CreatedApiKeyResult(saved, plainKey);
    }

    // ── INTERNAL HELPERS ─────────────────────────────────────────────────────

    /**
     * Generates a cryptographically strong random token prefixed with the key type.
     *
     * <p>Token format: {@code lum_user_<base64url(32 bytes)>} or
     * {@code lum_partner_<base64url(32 bytes)>}.
     * 32 bytes of entropy → ~256 bits, far exceeding the brute-force threshold.
     * SHA-256 (not Argon2) is appropriate because the token itself is already
     * high-entropy; slow hashing provides no benefit here.
     */
    private String generateKey(ApiKey.KeyType type) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String prefix = type == ApiKey.KeyType.USER ? "lum_user_" : "lum_partner_";
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
