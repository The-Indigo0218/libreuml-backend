package com.libreuml.backend.application.apikey.dto;

import com.libreuml.backend.domain.model.ApiKey;

/**
 * Carries both the persisted {@link ApiKey} and the one-time plain-text token.
 *
 * <p>The {@code plainKey} must be returned to the caller immediately and must
 * never be stored or logged; the persisted record only retains the SHA-256 digest.
 */
public record CreatedApiKeyResult(ApiKey apiKey, String plainKey) {}
