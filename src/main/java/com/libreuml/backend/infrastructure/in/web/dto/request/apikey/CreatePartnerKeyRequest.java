package com.libreuml.backend.infrastructure.in.web.dto.request.apikey;

import com.libreuml.backend.domain.model.ApiKey;
import jakarta.validation.constraints.*;

public record CreatePartnerKeyRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ApiKey.Scope scope,
        @NotBlank @Size(max = 255) String partnerName,
        @Email @Size(max = 255)    String partnerEmail,
        @Min(1) @Max(100_000)      int    rateLimitRead,
        @Min(1) @Max(100_000)      int    rateLimitWrite,
        @Size(max = 50)            String redemptionCode,
        @Min(1)                    Integer redemptionLimit,
        @Size(max = 2000)          String notes
) {}
