package com.libreuml.backend.infrastructure.in.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Resolves the real client IP, trusting the {@code X-Forwarded-For} header only when the
 * direct peer ({@link HttpServletRequest#getRemoteAddr()}) is a configured trusted proxy.
 *
 * <p>Without this guard any client can spoof {@code X-Forwarded-For} and obtain a fresh
 * rate-limit bucket on every request, defeating the brute-force and registration limits.
 *
 * <p>The trusted set is read from {@code app.security.trusted-proxies} (CIDRs or single IPs).
 * When the peer is trusted, the header is walked right-to-left and the first non-trusted
 * address is returned — the rightmost untrusted hop is the closest one the proxies actually
 * observed, so it cannot be forged by the client prepending values.
 */
@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final List<IpAddressMatcher> trustedProxies;

    public ClientIpResolver(
            @Value("${app.security.trusted-proxies:127.0.0.1,::1}") String trustedProxies) {
        this.trustedProxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(cidr -> !cidr.isBlank())
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only honour forwarding headers when the request actually came through a trusted proxy.
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] hops = forwardedFor.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidate = hops[i].trim();
            if (!candidate.isBlank() && !isTrustedProxy(candidate)) {
                return candidate;
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        for (IpAddressMatcher matcher : trustedProxies) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // Address family mismatch (e.g. IPv4 matcher vs IPv6 candidate) — not a match.
            }
        }
        return false;
    }
}
