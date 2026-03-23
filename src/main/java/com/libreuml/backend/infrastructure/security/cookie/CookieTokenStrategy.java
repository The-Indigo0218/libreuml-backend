package com.libreuml.backend.infrastructure.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class CookieTokenStrategy {

    // The __Host- prefix enforces: Secure=true, Path=/, no Domain attribute (RFC 6265bis §4.1.3).
    // Any deviation from these attributes causes browsers to silently reject the cookie.
    private static final String ACCESS_TOKEN_COOKIE  = "__Host-jwt";
    private static final String REFRESH_TOKEN_COOKIE = "__Host-refresh";

    private static final int ACCESS_TOKEN_MAX_AGE_SECONDS  = 15 * 60;       // 15 minutes
    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7 days

    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_COOKIE, token, ACCESS_TOKEN_MAX_AGE_SECONDS).toString());
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN_COOKIE, token, REFRESH_TOKEN_MAX_AGE_SECONDS).toString());
    }

    public String extractTokenFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, ACCESS_TOKEN_COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, REFRESH_TOKEN_COOKIE);
        return cookie != null ? cookie.getValue() : null;
    }

    public void clearTokenCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(ACCESS_TOKEN_COOKIE).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(REFRESH_TOKEN_COOKIE).toString());
    }

    private ResponseCookie buildCookie(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
