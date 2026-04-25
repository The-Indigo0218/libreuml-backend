package com.libreuml.backend.infrastructure.in.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the real client IP from an HTTP request.
 * X-Forwarded-For is only trusted when the direct connection comes from a
 * configured trusted proxy (e.g. nginx, Cloudflare, ALB). Arbitrary clients
 * cannot spoof the header to bypass rate limiting or poison audit logs.
 */
@Component
public class TrustedProxyResolver {

    private final List<String> trustedProxies;

    public TrustedProxyResolver(
            @Value("${app.security.trusted-proxies:127.0.0.1,::1}") List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
