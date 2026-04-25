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
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Stricter rate limit for {@code POST /api/v1/auth/email/verify/send}: 1 request per 60 seconds per IP.
 * Prevents email-spam abuse against a valid account. Runs at priority 3.
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class EmailVerifyRateLimitFilter extends OncePerRequestFilter {

    private static final String VERIFY_SEND_PATH = "/api/v1/auth/email/verify/send";

    private final TrustedProxyResolver trustedProxyResolver;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        boolean isVerifySend = VERIFY_SEND_PATH.equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod());

        if (!isVerifySend) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = trustedProxyResolver.resolveClientIp(request);
        Bucket bucket = buckets.get(clientIp, ip -> buildBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Verification email already sent. Wait 60 seconds before requesting again.\","
                    + "\"timestamp\":\"" + Instant.now() + "\","
                    + "\"path\":\"" + request.getRequestURI() + "\"}");
        }
    }

    private Bucket buildBucket() {
        Bandwidth limit = Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(60)));
        return Bucket.builder().addLimit(limit).build();
    }
}
