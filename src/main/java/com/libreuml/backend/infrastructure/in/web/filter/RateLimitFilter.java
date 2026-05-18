package com.libreuml.backend.infrastructure.in.web.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter for authentication and administrative endpoints.
 *
 * - /api/v1/auth/** - 10 requests per minute per IP (prevent brute force)
 * - /api/v1/reports (admin endpoints) - 30 requests per minute per IP
 *
 * Applied at filter priority 1, before the Spring Security filter chain.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_RATE_LIMIT = 10;
    private static final int ADMIN_RATE_LIMIT = 30;
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/reports";

    private final Cache<String, Bucket> authBuckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    private final Cache<String, Bucket> adminBuckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith(AUTH_PATH_PREFIX)) {
            applyRateLimit(request, response, filterChain, authBuckets, AUTH_RATE_LIMIT, "Auth");
        } else if (uri.startsWith(ADMIN_PATH_PREFIX) && isAdminEndpoint(request)) {
            applyRateLimit(request, response, filterChain, adminBuckets, ADMIN_RATE_LIMIT, "Admin");
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isAdminEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return "GET".equals(method) && uri.equals("/api/v1/reports")
                || "PATCH".equals(method) && uri.matches(".*/reports/.*/(status|priority|respond)$");
    }

    private void applyRateLimit(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            Cache<String, Bucket> buckets,
            int rateLimit,
            String type
    ) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.get(clientIp, ip -> buildBucket(rateLimit));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response, type);
        }
    }

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String type) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too many requests for " + type + " endpoint. Please wait before trying again.\"}");
    }
}
