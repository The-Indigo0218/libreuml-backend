package com.libreuml.backend.application.auth.port.out;

import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.dto.OAuthUserInfo;

/**
 * Output port (driven port) for OAuth2 provider communication.
 *
 * <p>Each concrete adapter in {@code infrastructure/out/oauth/} implements this interface
 * for one specific provider.  The application layer stays completely ignorant of HTTP
 * transport, JSON parsing, or provider-specific URL formats — all of that lives
 * exclusively in the adapters.
 *
 * <p>The {@link #provider()} discriminator lets the {@code OAuthLoginService} route to
 * the correct adapter from a {@code List<OAuthProviderPort>} injected by Spring,
 * keeping the service open for extension (new providers) without modification.
 */
public interface OAuthProviderPort {

    OAuthProvider provider();

    OAuthUserInfo fetchUserInfo(String authorizationCode, String redirectUri);

    String buildAuthorizationUrl(String state, String redirectUri);
}
