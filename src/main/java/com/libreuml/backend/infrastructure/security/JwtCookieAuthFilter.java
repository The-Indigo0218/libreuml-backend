package com.libreuml.backend.infrastructure.security;

import com.libreuml.backend.application.user.port.out.TokenProviderPort;
import com.libreuml.backend.infrastructure.security.cookie.CookieTokenStrategy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Hybrid authentication filter supporting two token transports:
 * 1. Web clients — reads from the {@code __Host-jwt} HttpOnly cookie (XSS-safe).
 * 2. API clients (Electron, MCP agents) — falls back to {@code Authorization: Bearer <token>}.
 *
 * After extracting the token, it validates the {@code pwdVersion} claim against the current
 * DB value to immediately invalidate tokens issued before a password change.
 */
@Component
@RequiredArgsConstructor
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProvider;
    private final UserDetailsService userDetailsService;
    private final CookieTokenStrategy cookieTokenStrategy;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (!tokenProvider.validateToken(token)) {
                writeUnauthorized(request, response, "Invalid or expired token.");
                return;
            }

            String email = tokenProvider.getEmailFromToken(token);
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

            int tokenPwdVersion = tokenProvider.getPwdVersionFromToken(token);
            if (tokenPwdVersion != userDetails.getPasswordVersion()) {
                writeUnauthorized(request, response, "Token invalidated. Please log in again.");
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"" + message + "\","
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"path\":\"" + request.getRequestURI() + "\"}");
    }

    private String resolveToken(HttpServletRequest request) {
        String fromCookie = cookieTokenStrategy.extractTokenFromCookie(request);
        if (StringUtils.hasText(fromCookie)) {
            return fromCookie;
        }

        String bearerHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerHeader) && bearerHeader.startsWith("Bearer ")) {
            return bearerHeader.substring(7);
        }

        return null;
    }
}
