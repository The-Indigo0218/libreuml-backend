package com.libreuml.backend.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreuml.backend.AbstractIntegrationTest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.LoginRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthSecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REFRESH_URL  = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL   = "/api/v1/auth/logout";
    private static final String ME_URL       = "/api/v1/users/me";

    private static final String TEST_PASSWORD = "Test@1234Valid";

    // Each test method gets its own synthetic IP so rate-limit buckets never bleed across tests.
    // The Caffeine caches inside RateLimitFilter/RegisterRateLimitFilter are singleton Spring beans
    // that live for the entire test run; unique IPs guarantee per-test isolation without requiring
    // cache eviction between methods.
    private static final AtomicInteger TEST_ID = new AtomicInteger(1);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String testEmail;
    private String testIp;

    @BeforeEach
    void setUp() throws Exception {
        int id = TEST_ID.getAndIncrement();
        testEmail = "security-test-" + id + "@libreuml.io";
        // 10.x.y format — unique per test, never conflicts with real loopback or production ranges
        testIp = "10." + (id / 256) + "." + (id % 256) + ".1";

        mockMvc.perform(post(REGISTER_URL)
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("Security Tester", testEmail, TEST_PASSWORD, "STUDENT"))))
                .andExpect(status().isCreated());
    }

    @Test
    void login_setsHttpOnlyCookie_andReturnsNoBody() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, TEST_PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie accessCookie = result.getResponse().getCookie("__Host-jwt");
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(accessCookie.getValue()).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    void login_setsRefreshTokenCookie() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, TEST_PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie("__Host-refresh");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getValue()).isNotBlank();
    }

    @Test
    void protectedEndpoint_withValidCookie_returns200() throws Exception {
        Cookie[] cookies = loginAndGetCookies();

        mockMvc.perform(get(ME_URL)
                .header("X-Forwarded-For", testIp)
                .cookie(cookies))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_rotatesTokens_andReturnsNewCookies() throws Exception {
        Cookie[] cookies = loginAndGetCookies();
        Cookie refreshCookie = findCookie(cookies, "__Host-refresh");

        MvcResult refreshResult = mockMvc.perform(post(REFRESH_URL)
                .header("X-Forwarded-For", testIp)
                .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie newAccess  = refreshResult.getResponse().getCookie("__Host-jwt");
        Cookie newRefresh = refreshResult.getResponse().getCookie("__Host-refresh");
        assertThat(newAccess).isNotNull();
        assertThat(newRefresh).isNotNull();
        assertThat(newRefresh.getValue()).isNotEqualTo(refreshCookie.getValue());
    }

    @Test
    void logout_clearsCookies() throws Exception {
        Cookie[] cookies = loginAndGetCookies();

        MvcResult result = mockMvc.perform(delete(LOGOUT_URL)
                .header("X-Forwarded-For", testIp)
                .cookie(cookies))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie accessCookie = result.getResponse().getCookie("__Host-jwt");
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie.getMaxAge()).isZero();
    }

    @Test
    void rateLimitFilter_returns429AfterTenRequests() throws Exception {
        // @BeforeEach already consumed 1 token for testIp (register call).
        // 9 more auth calls exhaust the 10/min bucket, then the 10th returns 429.
        for (int i = 0; i < 9; i++) {
            mockMvc.perform(post(LOGIN_URL)
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("wrong@email.com", "wrong"))))
                    .andExpect(status().is4xxClientError());
        }
        mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("wrong@email.com", "wrong"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mockMvc.perform(get(ME_URL)
                .header("X-Forwarded-For", testIp)
                .cookie(new Cookie("__Host-jwt", "not.a.valid.jwt.token")))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private Cookie[] loginAndGetCookies() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .header("X-Forwarded-For", testIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(testEmail, TEST_PASSWORD))))
                .andReturn();
        return result.getResponse().getCookies();
    }

    private Cookie findCookie(Cookie[] cookies, String name) {
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c;
        }
        throw new AssertionError("Cookie not found: " + name);
    }
}
