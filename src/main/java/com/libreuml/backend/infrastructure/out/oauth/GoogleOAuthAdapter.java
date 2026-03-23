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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter that implements the Google OAuth2 Authorization Code flow manually via
 * Spring's {@link RestClient}.
 *
 * <p>Two-step protocol:
 * <ol>
 *   <li>Exchange the authorization {@code code} for an access token via
 *       {@code POST https://oauth2.googleapis.com/token}.</li>
 *   <li>Fetch the authenticated user's profile via
 *       {@code GET https://www.googleapis.com/oauth2/v3/userinfo}.</li>
 * </ol>
 *
 * <p>Google's userinfo endpoint returns {@code email_verified} directly in the payload,
 * so unlike GitHub, no secondary call is required to obtain the verified email status.
 * The {@code sub} field is Google's stable, immutable user identifier (equivalent to
 * GitHub's numeric {@code id}).
 */
@Component
public class GoogleOAuthAdapter implements OAuthProviderPort {

    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";
    private static final String USER_URL      = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    @Value("${app.oauth.google.client-id:}")
    private String clientId;

    @Value("${app.oauth.google.client-secret:}")
    private String clientSecret;

    private final RestClient restClient;

    public GoogleOAuthAdapter(RestClient.Builder builder) {
        this.restClient = builder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo fetchUserInfo(String authorizationCode, String redirectUri) {
        String accessToken = exchangeCodeForToken(authorizationCode, redirectUri);
        GoogleUserInfo profile = fetchProfile(accessToken);

        return new OAuthUserInfo(
                OAuthProvider.GOOGLE,
                profile.sub(),
                profile.email(),
                profile.emailVerified(),
                profile.name() != null ? profile.name() : profile.email(),
                profile.picture()
        );
    }

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        return AUTHORIZE_URL
                + "?client_id="     + clientId
                + "&redirect_uri="  + redirectUri
                + "&response_type=code"
                + "&scope=openid+email+profile"
                + "&state="         + state
                + "&access_type=offline";
    }

    private String exchangeCodeForToken(String code, String redirectUri) {
        try {
            // Google's token endpoint requires application/x-www-form-urlencoded
            MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
            formBody.add("client_id",     clientId);
            formBody.add("client_secret", clientSecret);
            formBody.add("code",          code);
            formBody.add("redirect_uri",  redirectUri);
            formBody.add("grant_type",    "authorization_code");

            GoogleTokenResponse tokenResponse = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (req, res) -> { throw new OAuthException("Google token exchange failed: " + res.getStatusCode()); })
                    .body(GoogleTokenResponse.class);

            if (tokenResponse == null || tokenResponse.accessToken() == null) {
                throw new OAuthException("Google returned an empty or invalid access token");
            }
            return tokenResponse.accessToken();
        } catch (RestClientException e) {
            throw new OAuthException("Failed to contact Google token endpoint", e);
        }
    }

    private GoogleUserInfo fetchProfile(String accessToken) {
        try {
            GoogleUserInfo user = restClient.get()
                    .uri(USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(),
                            (req, res) -> { throw new OAuthException("Google userinfo endpoint returned: " + res.getStatusCode()); })
                    .body(GoogleUserInfo.class);

            if (user == null) {
                throw new OAuthException("Google returned an empty user profile");
            }
            return user;
        } catch (RestClientException e) {
            throw new OAuthException("Failed to fetch Google user profile", e);
        }
    }

    // ---- Internal response records (not part of the domain or application contracts) ----

    private record GoogleTokenResponse(
            @JsonProperty("access_token")  String accessToken,
            @JsonProperty("token_type")    String tokenType,
            @JsonProperty("expires_in")    int expiresIn,
            String scope
    ) {}

    private record GoogleUserInfo(
            String sub,
            String email,
            @JsonProperty("email_verified") boolean emailVerified,
            String name,
            String picture
    ) {}
}
