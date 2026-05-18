package com.libreuml.backend.infrastructure.in.web.dto.request.apikey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePartnerKeyLimitsRequest(
        @NotNull @Min(1) @Max(100_000) Integer rateLimitRead,
        @NotNull @Min(1) @Max(100_000) Integer rateLimitWrite
) {}
