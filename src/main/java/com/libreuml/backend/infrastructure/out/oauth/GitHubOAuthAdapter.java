package com.libreuml.backend.infrastructure.out.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.dto.OAuthUserInfo;
import com.libreuml.backend.application.auth.exception.OAuthException;
import com.libreuml.backend.application.auth.port.out.OAuthProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Adapter that implements the GitHub OAuth2 Authorization Code flow manually via
 * Spring's {@link RestClient}.
 *
 * <p>Two-step protocol:
 * <ol>
 *   <li>Exchange the authorization {@code code} for an access token via
 *       {@code POST https://github.com/login/oauth/access_token}.</li>
 *   <li>Fetch the authenticated user's profile via
 *       {@code GET https://api.github.com/user}.</li>
 * </ol>
 *
 * <p>If the user's email is not public (GitHub allows keeping it private), a third call
 * to {@code GET https://api.github.com/user/emails} retrieves the primary verified
 * address.  This ensures we always have a valid email for account matching.
 */
@Component
public class GitHubOAuthAdapter implements OAuthProviderPort {

    private static final String TOKEN_URL     = "https://github.com/login/oauth/access_token";
    private static final String USER_URL      = "https://api.github.com/user";
    private static final String EMAILS_URL    = "https://api.github.com/user/emails";
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";

    @Value("${app.oauth.github.client-id:}")
    private String clientId;

    @Value("${app.oauth.github.client-secret:}")
    private String clientSecret;

    private final RestClient restClient;

    public GitHubOAuthAdapter(RestClient.Builder builder) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GITHUB;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForToken(authorizationCode, redirectUri);
        GitHubUser profile = fetchProfile(accessToken);

        String email = profile.email();
        if (email == null || email.isBlank()) {
            email = fetchPrimaryVerifiedEmail(accessToken);
        }

        return new OAuthUserInfo(
                OAuthProvider.GITHUB,
                String.valueOf(profile.id()),
                email,
                true,  // GitHub's primary emails are always verified
                profile.name() != null ? profile.name() : profile.login(),
                profile.avatarUrl()
        );
    }

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        return AUTHORIZE_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=user:email"
                + "&state=" + state;
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        try {
            GitHubTokenResponse tokenResponse = restClient.post()
                    .uri(TOKEN_URL)
                    .body(Map.of(
                            "client_id",     clientId,
                            "client_secret", clientSecret,
                            "code",          code,
                            "redirect_uri",  redirectUri
                    ))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (req, res) -> { throw new OAuthException("GitHub token exchange failed: " + res.getStatusCode()); })
                    .body(GitHubTokenResponse.class);

            if (tokenResponse == null || tokenResponse.accessToken() == null) {
                throw new OAuthException("GitHub returned an empty or invalid access token");
            }
            return tokenResponse.accessToken();
        } catch (RestClientException e) {
            throw new OAuthException("Failed to contact GitHub token endpoint", e);
        }
    }

    private GitHubUser fetchProfile(String accessToken) {
        try {
            GitHubUser user = restClient.get()
                    .uri(USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (req, res) -> { throw new OAuthException("GitHub user endpoint returned: " + res.getStatusCode()); })
                    .body(GitHubUser.class);

            if (user == null) {
                throw new OAuthException("GitHub returned an empty user profile");
            }
            return user;
        } catch (RestClientException e) {
            throw new OAuthException("Failed to fetch GitHub user profile", e);
        }
    }

    private String fetchPrimaryVerifiedEmail(String accessToken) {
        try {
            List<GitHubEmail> emails = restClient.get()
                    .uri(EMAILS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<>() {});

            if (emails == null) {
                throw new OAuthException("GitHub returned no email list");
            }

            return emails.stream()
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmail::email)
                    .findFirst()
                    .orElseThrow(() -> new OAuthException(
                            "No primary verified email found in GitHub account. " +
                            "Please add and verify a primary email on GitHub."));
        } catch (RestClientException e) {
            throw new OAuthException("Failed to fetch GitHub emails", e);
        }
    }

    // ---- Internal response records (not part of the domain or application contracts) ----

    private record GitHubTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType,
            String scope
    ) {}

    private record GitHubUser(
            long id,
            String login,
            String name,
            String email,
            @JsonProperty("avatar_url") String avatarUrl
    ) {}

    private record GitHubEmail(
            String email,
            boolean verified,
            boolean primary
    ) {}
}
