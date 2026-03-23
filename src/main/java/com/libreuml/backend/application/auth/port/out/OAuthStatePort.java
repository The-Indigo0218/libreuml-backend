package com.libreuml.backend.application.auth.port.out;

import com.libreuml.backend.application.auth.exception.InvalidOAuthStateException;

/**
 * Output port for OAuth2 CSRF state token lifecycle.
 *
 * <p>The state parameter (RFC 6749 §4.1.1) prevents cross-site request forgery by tying
 * the authorization request to the callback.  The infrastructure adapter uses HMAC-SHA256
 * to create a self-contained, verifiable, time-limited token — no server-side session
 * or cache is required, preserving the stateless nature of the API.
 */
public interface OAuthStatePort {

    String generateState();

    /** @throws InvalidOAuthStateException if the state is malformed, tampered, or expired */
    void validateState(String state);
}
