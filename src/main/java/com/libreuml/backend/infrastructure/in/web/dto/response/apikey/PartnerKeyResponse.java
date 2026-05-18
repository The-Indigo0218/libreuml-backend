package com.libreuml.backend.infrastructure.in.web.dto.response.apikey;

import java.time.Instant;
import java.util.UUID;

/** Lightweight partner key representation used in paginated list responses. */
public record PartnerKeyResponse(
        UUID    id,
        String  name,
        String  keyPrefix,
        String  scope,
        String  partnerName,
        int     rateLimitRead,
        int     rateLimitWrite,
        String  redemptionCode,
        int     redemptionCount,
        Instant createdAt,
        long    usageCount,
        boolean active
) {}
