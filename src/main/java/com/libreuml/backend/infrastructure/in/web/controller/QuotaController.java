package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.user.port.in.GetUserUseCase;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.infrastructure.in.web.dto.QuotaResponse;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the per-user storage quota summary.
 *
 * <p>Kept as a dedicated controller (separate from {@link UserController}) to keep
 * each controller cohesive and allow the quota surface to grow independently.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class QuotaController {

    private final GetUserUseCase getUserUseCase;

    /**
     * Returns the authenticated user's storage quota, current usage, and remaining capacity.
     *
     * <p>Example response:
     * <pre>{@code {"quota":10485760,"used":524288,"available":9961472}}</pre>
     */
    @GetMapping("/me/quota")
    public ResponseEntity<QuotaResponse> getQuota(
            @AuthenticationPrincipal CustomUserDetails principal) {
        User user = getUserUseCase.getUserById(principal.getId());
        long quota = user.getStorageQuotaBytes();
        long used  = user.getStorageUsedBytes();
        return ResponseEntity.ok(new QuotaResponse(quota, used, quota - used));
    }
}
