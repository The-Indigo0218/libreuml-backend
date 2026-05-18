package com.libreuml.backend.infrastructure.in.web.dto.response.apikey;

import java.time.Instant;
import java.util.UUID;

/** Full partner key view including usage stats, returned by the admin GET-by-id endpoint. */
public record PartnerKeyDetailResponse(
        UUID    id,
        String  name,
        String  keyPrefix,
        String  scope,
        String  partnerName,
        String  partnerEmail,
        int     rateLimitRead,
        int     rateLimitWrite,
        String  redemptionCode,
        Integer redemptionLimit,
        int     redemptionCount,
        Instant createdAt,
        Instant lastUsedAt,
        long    usageCount,
        boolean active,
        Instant revokedAt,
        String  notes
) {}
