package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.AbstractIntegrationTest;
import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.application.auth.dto.OAuthUserInfo;
import com.libreuml.backend.application.auth.port.out.OAuthStatePort;
import com.libreuml.backend.infrastructure.out.oauth.GitHubOAuthAdapter;
import com.libreuml.backend.infrastructure.out.oauth.GoogleOAuthAdapter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the OAuth2 authorization code flow.
 *
 * <p>The real {@link GitHubOAuthAdapter} and {@link GoogleOAuthAdapter} beans are replaced with
 * Mockito mocks ({@code @MockBean}) so no outbound HTTP calls are made during test execution.
 * The {@link OAuthStatePort} ({@link com.libreuml.backend.infrastructure.security.OAuthStateSigner})
 * is left as a real bean so that state generation and HMAC validation work exactly as in production.
 */
class OAuthIntegrationTest extends AbstractIntegrationTest {

    // Start high to avoid IP collisions with AuthSecurityIntegrationTest's counter.
    private static final AtomicInteger TEST_ID = new AtomicInteger(5000);

    private static final String CALLBACK_URL = "http://localhost/api/v1/oauth/github/callback";

    @MockBean
    GitHubOAuthAdapter gitHubOAuthAdapter;

    @MockBean
    GoogleOAuthAdapter googleOAuthAdapter;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OAuthStatePort oAuthStatePort;

    @BeforeEach
    void configureMocks() {
        // provider() discriminator must be correct for the OAuthLoginService adapter resolution
        when(gitHubOAuthAdapter.provider()).thenReturn(OAuthProvider.GITHUB);
        when(googleOAuthAdapter.provider()).thenReturn(OAuthProvider.GOOGLE);

        when(gitHubOAuthAdapter.buildAuthorizationUrl(any(), any()))
                .thenAnswer(inv -> "https://github.com/login/oauth/authorize?state=" + inv.getArgument(0));
        when(googleOAuthAdapter.buildAuthorizationUrl(any(), any()))
                .thenAnswer(inv -> "https://accounts.google.com/o/oauth2/v2/auth?state=" + inv.getArgument(0));
    }

    // ---- authorize endpoint ----

    @Test
    void authorizeGitHub_returnsAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/github/authorize")
                        .param("redirectUri", CALLBACK_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(
                        org.hamcrest.Matchers.containsString("github.com")));
    }

    @Test
    void authorizeGoogle_returnsAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/google/authorize")
                        .param("redirectUri", "http://localhost/api/v1/oauth/google/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value(
                        org.hamcrest.Matchers.containsString("accounts.google.com")));
    }

    @Test
    void authorizeWithUnknownProvider_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/twitter/authorize")
                        .param("redirectUri", "http://localhost/callback"))
                .andExpect(status().isBadRequest());
    }

    // ---- callback endpoint — happy path ----

    @Test
    void githubCallback_withValidStateAndCode_setsCookiesAndRedirects() throws Exception {
        int id = TEST_ID.getAndIncrement();
        String testEmail = "oauth-gh-" + id + "@libreuml.io";
        String testIp    = "10.99." + (id / 256) + "." + (id % 256);

        when(gitHubOAuthAdapter.fetchUserInfo(eq("valid-code"), any()))
                .thenReturn(new OAuthUserInfo(
                        OAuthProvider.GITHUB, "gh-" + id,
                        testEmail, true, "GitHub User " + id, null));

        String state = oAuthStatePort.generateState();

        MvcResult result = mockMvc.perform(get("/api/v1/oauth/github/callback")
                        .header("X-Forwarded-For", testIp)
                        .param("code",  "valid-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.notNullValue()))
                .andReturn();

        Cookie accessCookie  = result.getResponse().getCookie("__Host-jwt");
        Cookie refreshCookie = result.getResponse().getCookie("__Host-refresh");

        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(accessCookie.getValue()).isNotBlank();

        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getValue()).isNotBlank();
    }

    @Test
    void googleCallback_withValidStateAndCode_setsCookies() throws Exception {
        int id = TEST_ID.getAndIncrement();
        String testEmail = "oauth-google-" + id + "@libreuml.io";
        String testIp    = "10.99." + (id / 256) + "." + (id % 256);

        when(googleOAuthAdapter.fetchUserInfo(eq("google-code"), any()))
                .thenReturn(new OAuthUserInfo(
                        OAuthProvider.GOOGLE, "google-sub-" + id,
                        testEmail, true, "Google User " + id, null));

        String state = oAuthStatePort.generateState();

        MvcResult result = mockMvc.perform(get("/api/v1/oauth/google/callback")
                        .header("X-Forwarded-For", testIp)
                        .param("code",  "google-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getCookie("__Host-jwt")).isNotNull();
        assertThat(result.getResponse().getCookie("__Host-refresh")).isNotNull();
    }

    // ---- callback endpoint — error cases ----

    @Test
    void callback_withMalformedState_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/github/callback")
                        .param("code",  "any-code")
                        .param("state", "not-a-valid-state-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_withTamperedStateSignature_returns400() throws Exception {
        String validState = oAuthStatePort.generateState();
        // Replace the HMAC segment with garbage to simulate a tampered token
        String tamperedState = validState.substring(0, validState.lastIndexOf('.') + 1) + "AAAAAAAAAAAA";

        mockMvc.perform(get("/api/v1/oauth/github/callback")
                        .param("code",  "any-code")
                        .param("state", tamperedState))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_withUnknownProvider_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/linkedin/callback")
                        .param("code",  "any-code")
                        .param("state", oAuthStatePort.generateState()))
                .andExpect(status().isBadRequest());
    }
}
