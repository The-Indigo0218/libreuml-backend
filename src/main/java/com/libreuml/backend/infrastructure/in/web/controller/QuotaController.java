package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.project.port.in.GetStorageQuotaUseCase;
import com.libreuml.backend.infrastructure.in.web.dto.QuotaResponse;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class QuotaController {

    private final GetStorageQuotaUseCase getStorageQuotaUseCase;

    @GetMapping("/me/quota")
    public ResponseEntity<QuotaResponse> getQuota(
            @AuthenticationPrincipal CustomUserDetails principal) {
        GetStorageQuotaUseCase.QuotaInfo info = getStorageQuotaUseCase.getQuota(principal.getId());
        return ResponseEntity.ok(new QuotaResponse(
                info.quota(), info.used(), info.available(),
                new QuotaResponse.Breakdown(info.modelsBytes(), info.diagramsBytes())));
    }
}
