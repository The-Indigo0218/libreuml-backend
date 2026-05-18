package com.libreuml.backend.infrastructure.in.web.dto.response.apikey;

import java.time.Instant;
import java.util.UUID;

/**
 * Used for listing a user's own keys. Never includes the plain-text token.
 * {@code keyPrefix} shows the first 15 characters (e.g. {@code lum_user_AbCde}).
 */
public record ApiKeyResponse(
        UUID    id,
        String  name,
        String  keyPrefix,
        String  keyType,
        String  scope,
        int     rateLimitRead,
        int     rateLimitWrite,
        Instant createdAt,
        Instant lastUsedAt,
        long    usageCount,
        boolean active
) {}
