package com.libreuml.backend.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.libreuml.backend.application.apikey.port.out.ApiKeyRepository;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.ApiKey;
import com.libreuml.backend.domain.model.User;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Spring Security filter that authenticates requests carrying
 * {@code Authorization: ApiKey <token>} headers.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Skip if the header is absent or does not start with {@code "ApiKey "}, or if the
 *       security context is already populated (e.g. by the JWT cookie filter).</li>
 *   <li>SHA-256 hash the raw token; look it up in the database.</li>
 *   <li>Reject (401) if not found or inactive.</li>
 *   <li>Reject (403) if the key's scope does not permit the HTTP method.</li>
 *   <li>Consume one token from the per-key Bucket4j bucket; reject (429) if exhausted.</li>
 *   <li>Populate the Spring Security context:
 *       <ul>
 *         <li>Key linked to a user → load user, set {@link UsernamePasswordAuthenticationToken}
 *             with {@link CustomUserDetails} (transparent to existing controllers).</li>
 *         <li>Unlinked partner key → set {@link ApiKeyAuthentication} for machine-to-machine
 *             endpoints.</li>
 *       </ul>
 *   </li>
 *   <li>Trigger an async, fire-and-forget usage update (last_used_at + usage_count++).</li>
 * </ol>
 *
 * <h3>Rate limiting</h3>
 * Per-key Bucket4j buckets are backed by an in-memory Caffeine cache that expires entries
 * after 25 hours (slightly longer than one day so a bucket is reused across the same day
 * even if requests span midnight). Limits are read from the key record at bucket-creation
 * time; a limit change takes effect when the cached bucket expires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String APIKEY_PREFIX = "ApiKey ";

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository   userRepository;

    /**
     * Separate caches for READ and WRITE buckets so each scope has its own daily quota.
     * Keys expire 25 h after last access — just over a full day — to avoid premature
     * eviction for keys used less than once per day while still reclaiming memory.
     */
    private final Cache<UUID, Bucket> readBuckets = Caffeine.newBuilder()
            .expireAfterAccess(25, TimeUnit.HOURS)
            .build();

    private final Cache<UUID, Bucket> writeBuckets = Caffeine.newBuilder()
            .expireAfterAccess(25, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Pass through if no ApiKey header is present or if JWT auth already ran.
        if (header == null || !header.startsWith(APIKEY_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken  = header.substring(APIKEY_PREFIX.length()).strip();
        String hashedKey = hashKey(rawToken);

        Optional<ApiKey> maybeKey = apiKeyRepository.findByHashedKey(hashedKey);

        if (maybeKey.isEmpty() || !maybeKey.get().isActive()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or inactive API key.");
            return;
        }

        ApiKey apiKey = maybeKey.get();

        // Scope check: READ keys may only perform GET requests.
        if (!apiKey.canPerform(request.getMethod())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "API key scope '" + apiKey.getScope() + "' does not permit "
                    + request.getMethod() + " requests.");
            return;
        }

        // Rate-limit check with Bucket4j.
        Bucket bucket = resolveBucket(apiKey);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        long resetEpochSeconds = Instant.now()
                .plusNanos(probe.getNanosToWaitForRefill())
                .getEpochSecond();

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));

        if (!probe.isConsumed()) {
            writeError(response, 429, "Rate limit exceeded. Resets at epoch " + resetEpochSeconds + ".");
            return;
        }

        // Populate Spring Security context.
        authenticate(apiKey, request);

        // Fire-and-forget usage tracking — must not block the response path.
        final UUID keyId = apiKey.getId();
        CompletableFuture.runAsync(() -> {
            try {
                apiKeyRepository.recordUsage(keyId);
            } catch (Exception ex) {
                log.warn("Failed to record usage for API key {}: {}", keyId, ex.getMessage());
            }
        });

        filterChain.doFilter(request, response);
    }

    private void authenticate(ApiKey apiKey, HttpServletRequest request) {
        if (apiKey.getUserId() != null) {
            userRepository.getUserById(apiKey.getUserId()).ifPresent(user -> {
                CustomUserDetails details = toUserDetails(user);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        } else {
            // Unlinked partner key — no user context available.
            ApiKeyAuthentication auth = new ApiKeyAuthentication(
                    apiKey,
                    List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    private CustomUserDetails toUserDetails(User user) {
        return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                user.getId(),
                user.getPasswordVersion());
    }

    private Bucket resolveBucket(ApiKey apiKey) {
        if (apiKey.getScope() == ApiKey.Scope.READ) {
            return readBuckets.get(apiKey.getId(),
                    id -> buildBucket(apiKey.getRateLimitRead()));
        }
        return writeBuckets.get(apiKey.getId(),
                id -> buildBucket(apiKey.getRateLimitWrite()));
    }

    private Bucket buildBucket(int requestsPerDay) {
        Bandwidth limit = Bandwidth.classic(
                requestsPerDay,
                Refill.intervally(requestsPerDay, Duration.ofDays(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String hashKey(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
