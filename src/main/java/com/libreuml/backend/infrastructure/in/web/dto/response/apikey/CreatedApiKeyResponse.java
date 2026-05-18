package com.libreuml.backend.infrastructure.in.web.dto.response.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned only on creation and redemption endpoints.
 * {@code plainKey} is the full token — it is displayed exactly once and never stored.
 */
public record CreatedApiKeyResponse(
        UUID    id,
        String  name,
        String  keyPrefix,
        String  keyType,
        String  scope,
        int     rateLimitRead,
        int     rateLimitWrite,
        Instant createdAt,
        String  plainKey
) {}
