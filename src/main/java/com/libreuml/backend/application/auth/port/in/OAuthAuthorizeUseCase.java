package com.libreuml.backend.application.auth.port.in;

import com.libreuml.backend.application.auth.dto.OAuthProvider;

public interface OAuthAuthorizeUseCase {

    /**
     * Generates a signed CSRF state token, embeds it in the provider's authorization URL,
     * and returns the complete URL that the user's browser should be redirected to.
     */
    String buildAuthorizationUrl(OAuthProvider provider, String redirectUri);
}
