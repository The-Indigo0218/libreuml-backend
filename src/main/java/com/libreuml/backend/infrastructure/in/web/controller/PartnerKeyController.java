package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.apikey.dto.*;
import com.libreuml.backend.application.apikey.port.in.*;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.ApiKey;
import com.libreuml.backend.infrastructure.in.web.dto.request.apikey.CreatePartnerKeyRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.apikey.UpdatePartnerKeyLimitsRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.apikey.UpdatePartnerKeyStatusRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.apikey.CreatedApiKeyResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.apikey.PartnerKeyDetailResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.apikey.PartnerKeyResponse;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only endpoints for managing PARTNER keys.
 * Access is restricted to {@code ROLE_ADMIN} in {@link com.libreuml.backend.infrastructure.security.config.SecurityConfig}.
 *
 * <ul>
 *   <li>{@code POST   /api/v1/admin/partner-keys}           — create key</li>
 *   <li>{@code GET    /api/v1/admin/partner-keys}           — paginated list</li>
 *   <li>{@code GET    /api/v1/admin/partner-keys/{id}}      — detailed view</li>
 *   <li>{@code PATCH  /api/v1/admin/partner-keys/{id}/limits}  — update rate limits</li>
 *   <li>{@code PATCH  /api/v1/admin/partner-keys/{id}/status}  — activate/deactivate</li>
 *   <li>{@code DELETE /api/v1/admin/partner-keys/{id}}      — permanent revocation</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/partner-keys")
@RequiredArgsConstructor
public class PartnerKeyController {

    private final CreatePartnerKeyUseCase       createPartnerKeyUseCase;
    private final ListPartnerKeysUseCase        listPartnerKeysUseCase;
    private final GetPartnerKeyUseCase          getPartnerKeyUseCase;
    private final UpdatePartnerKeyLimitsUseCase updatePartnerKeyLimitsUseCase;
    private final UpdatePartnerKeyStatusUseCase updatePartnerKeyStatusUseCase;
    private final RevokePartnerKeyUseCase       revokePartnerKeyUseCase;

    @PostMapping
    public ResponseEntity<CreatedApiKeyResponse> create(
            @RequestBody @Valid CreatePartnerKeyRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CreatedApiKeyResult result = createPartnerKeyUseCase.createPartnerKey(
                new CreatePartnerKeyCommand(
                        principal.getId(),
                        request.name(),
                        request.scope(),
                        request.partnerName(),
                        request.partnerEmail(),
                        request.rateLimitRead(),
                        request.rateLimitWrite(),
                        request.redemptionCode(),
                        request.redemptionLimit(),
                        request.notes()));

        return ResponseEntity.status(HttpStatus.CREATED).body(toCreatedResponse(result));
    }

    @GetMapping
    public ResponseEntity<PagedResult<PartnerKeyResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PagedResult<ApiKey> result = listPartnerKeysUseCase.listPartnerKeys(page, size);
        PagedResult<PartnerKeyResponse> response = new PagedResult<>(
                result.content().stream().map(this::toPartnerResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerKeyDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(toDetailResponse(getPartnerKeyUseCase.getPartnerKey(id)));
    }

    @PatchMapping("/{id}/limits")
    public ResponseEntity<PartnerKeyDetailResponse> updateLimits(
            @PathVariable UUID id,
            @RequestBody @Valid UpdatePartnerKeyLimitsRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ApiKey updated = updatePartnerKeyLimitsUseCase.updateLimits(
                new UpdatePartnerKeyLimitsCommand(
                        id, request.rateLimitRead(), request.rateLimitWrite(), principal.getId()));
        return ResponseEntity.ok(toDetailResponse(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PartnerKeyDetailResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdatePartnerKeyStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ApiKey updated = updatePartnerKeyStatusUseCase.updateStatus(
                new UpdatePartnerKeyStatusCommand(id, request.active(), principal.getId()));
        return ResponseEntity.ok(toDetailResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        revokePartnerKeyUseCase.revokePartnerKey(id, principal.getId());
        return ResponseEntity.noContent().build();
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

    private PartnerKeyResponse toPartnerResponse(ApiKey key) {
        return new PartnerKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getScope().name(),
                key.getPartnerName(),
                key.getRateLimitRead(),
                key.getRateLimitWrite(),
                key.getRedemptionCode(),
                key.getRedemptionCount(),
                key.getCreatedAt(),
                key.getUsageCount(),
                key.isActive());
    }

    private PartnerKeyDetailResponse toDetailResponse(ApiKey key) {
        return new PartnerKeyDetailResponse(
                key.getId(),
                key.getName(),
                key.getKeyPrefix(),
                key.getScope().name(),
                key.getPartnerName(),
                key.getPartnerEmail(),
                key.getRateLimitRead(),
                key.getRateLimitWrite(),
                key.getRedemptionCode(),
                key.getRedemptionLimit(),
                key.getRedemptionCount(),
                key.getCreatedAt(),
                key.getLastUsedAt(),
                key.getUsageCount(),
                key.isActive(),
                key.getRevokedAt(),
                key.getNotes());
    }
}
