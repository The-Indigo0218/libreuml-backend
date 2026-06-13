package com.libreuml.backend.application.auth.dto;

import com.libreuml.backend.domain.model.RefreshToken;

/**
 * Result of issuing a refresh token: the persistable {@link RefreshToken} (which stores only
 * the hash) paired with the raw opaque value that must be returned to the client exactly once.
 */
public record IssuedRefreshToken(String rawToken, RefreshToken token) {}
