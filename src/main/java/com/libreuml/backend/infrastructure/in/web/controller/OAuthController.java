package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.auth.dto.OAuthCallbackCommand;
import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.auth.port.in.OAuthAuthorizeUseCase;
import com.libreuml.backend.application.auth.port.in.OAuthLoginUseCase;
import com.libreuml.backend.infrastructure.in.web.dto.response.auth.OAuthAuthorizeResponse;
import com.libreuml.backend.infrastructure.security.cookie.CookieTokenStrategy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthAuthorizeUseCase oAuthAuthorizeUseCase;
    private final OAuthLoginUseCase oAuthLoginUseCase;
    private final CookieTokenStrategy cookieTokenStrategy;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Step 1 of the OAuth flow.
     *
     * <p>Returns a provider authorization URL containing a signed CSRF {@code state} token.
     * The caller (typically the SPA) must redirect the user's browser to this URL.
     *
     * @param provider    path segment — {@code github} or {@code google}
     * @param redirectUri the backend callback URL registered with the OAuth provider
     *                    (e.g. {@code https://api.libreuml.com/api/v1/oauth/github/callback})
     */
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<OAuthAuthorizeResponse> authorize(
            @PathVariable String provider,
            @RequestParam String redirectUri
    ) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        String authorizationUrl = oAuthAuthorizeUseCase.buildAuthorizationUrl(oAuthProvider, redirectUri);
        return ResponseEntity.ok(new OAuthAuthorizeResponse(authorizationUrl));
    }

    /**
     * Step 2 of the OAuth flow — the registered callback endpoint.
     *
     * <p>The OAuth provider redirects the user here with {@code code} and {@code state}.
     * This method validates the state, exchanges the code for tokens, provisions or links
     * the user account, sets the {@code __Host-jwt} and {@code __Host-refresh} cookies,
     * then performs a 302 redirect to the configured frontend URL.
     *
     * @param provider path segment — must match the adapter that handled step 1
     * @param code     the one-time authorization code from the provider
     * @param state    the HMAC-signed CSRF token generated during step 1
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OAuthProvider oAuthProvider = parseProvider(provider);

        var command = new OAuthCallbackCommand(
                code,
                state,
                buildCallbackUri(request, provider),
                oAuthProvider,
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );

        TokenPair tokens = oAuthLoginUseCase.login(command);
        cookieTokenStrategy.setAccessTokenCookie(response, tokens.accessToken());
        cookieTokenStrategy.setRefreshTokenCookie(response, tokens.rawRefreshToken());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendUrl)
                .build();
    }

    private OAuthProvider parseProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuthException("Unsupported OAuth provider: " + provider);
        }
    }

    private String buildCallbackUri(HttpServletRequest request, String provider) {
        return request.getScheme() + "://"
                + request.getServerName() + ":" + request.getServerPort()
                + "/api/v1/oauth/" + provider.toLowerCase() + "/callback";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
