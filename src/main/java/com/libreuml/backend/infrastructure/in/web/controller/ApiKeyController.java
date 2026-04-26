package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.apikey.dto.*;
import com.libreuml.backend.application.apikey.port.in.*;
import com.libreuml.backend.domain.model.ApiKey;
import com.libreuml.backend.infrastructure.in.web.dto.request.apikey.CreateApiKeyRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.apikey.RedeemPartnerCodeRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.apikey.ApiKeyResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.apikey.CreatedApiKeyResponse;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Self-service API key management for authenticated users.
 *
 * <ul>
 *   <li>{@code POST   /api/v1/users/me/api-keys}         — create USER key</li>
 *   <li>{@code GET    /api/v1/users/me/api-keys}         — list user's keys</li>
 *   <li>{@code DELETE /api/v1/users/me/api-keys/{id}}    — revoke own key</li>
 *   <li>{@code POST   /api/v1/users/me/redeem-partner-code} — redeem partner code</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiKeyController {

    private final CreateApiKeyUseCase       createApiKeyUseCase;
    private final ListUserApiKeysUseCase    listUserApiKeysUseCase;
    private final RevokeApiKeyUseCase       revokeApiKeyUseCase;
    private final RedeemPartnerCodeUseCase  redeemPartnerCodeUseCase;

    @PostMapping("/api-keys")
    public ResponseEntity<CreatedApiKeyResponse> create(
            @RequestBody @Valid CreateApiKeyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CreatedApiKeyResult result = createApiKeyUseCase.createUserKey(
                new CreateApiKeyCommand(principal.getId(), request.name(), request.scope()));

        return ResponseEntity.status(HttpStatus.CREATED).body(toCreatedResponse(result));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<ApiKeyResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<ApiKeyResponse> keys = listUserApiKeysUseCase.listByUser(principal.getId())
                .stream().map(this::toResponse).toList();

        return ResponseEntity.ok(keys);
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        revokeApiKeyUseCase.revoke(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/me/redeem-partner-code")
    public ResponseEntity<CreatedApiKeyResponse> redeemPartnerCode(
            @RequestBody @Valid RedeemPartnerCodeRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CreatedApiKeyResult result = redeemPartnerCodeUseCase.redeem(
                request.redemptionCode(), principal.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toCreatedResponse(result));
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private CreatedApiKeyResponse toCreatedResponse(CreatedApiKeyResult result) {
        ApiKey key = result.apiKey();
        return new CreatedApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getKeyType().name(),
                key.getScope().name(),
                key.getRateLimitRead(),
                key.getRateLimitWrite(),
                key.getCreatedAt(),
                result.plainKey());
    }

    private ApiKeyResponse toResponse(ApiKey key) {
        return new ApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getKeyType().name(),
                key.getScope().name(),
                key.getRateLimitRead(),
                key.getRateLimitWrite(),
                key.getCreatedAt(),
                key.getLastUsedAt(),
                key.getUsageCount(),
                key.isActive());
    }
}
