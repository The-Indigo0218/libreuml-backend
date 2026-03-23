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
 * Stricter rate limit for {@code POST /api/v1/auth/register}: 3 registrations per hour per IP.
 * Runs at priority 2, after {@link RateLimitFilter} has applied the general auth limit.
 */
@Component
@Order(2)
public class RegisterRateLimitFilter extends OncePerRequestFilter {

    private static final int REGISTRATIONS_PER_HOUR = 3;
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        boolean isRegisterPost = REGISTER_PATH.equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod());

        if (!isRegisterPost) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.get(clientIp, ip -> buildBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(response);
        }
    }

    private Bucket buildBucket() {
        Bandwidth limit = Bandwidth.classic(
                REGISTRATIONS_PER_HOUR,
                Refill.intervally(REGISTRATIONS_PER_HOUR, Duration.ofHours(1))
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

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Registration limit exceeded. Try again in an hour.\"}");
    }
}
