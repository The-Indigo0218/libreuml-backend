package com.libreuml.backend.infrastructure.in.web.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;

/**
 * Resolves the real client IP from an HTTP request.
 * X-Forwarded-For is only trusted when the direct connection comes from a
 * configured trusted proxy (e.g. nginx, Cloudflare, ALB). Arbitrary clients
 * cannot spoof the header to bypass rate limiting or poison audit logs.
 *
 * Entries in trusted-proxies support both exact IPs (127.0.0.1) and CIDR
 * ranges (172.0.0.0/8) so Docker bridge network subnets can be trusted when
 * nginx runs as a sidecar container.
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
        if (!isTrusted(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private boolean isTrusted(String remoteAddr) {
        for (String entry : trustedProxies) {
            if (entry.contains("/")) {
                if (isInCidr(remoteAddr, entry)) return true;
            } else {
                if (entry.equals(remoteAddr)) return true;
            }
        }
        return false;
    }

    private boolean isInCidr(String ipAddr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            byte[] remote  = InetAddress.getByName(ipAddr).getAddress();
            if (network.length != remote.length) return false;

            int prefix       = Integer.parseInt(parts[1]);
            int fullBytes    = prefix / 8;
            int remainingBits = prefix % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != remote[i]) return false;
            }
            if (remainingBits > 0 && fullBytes < network.length) {
                int mask = 0xFF << (8 - remainingBits);
                return (network[fullBytes] & mask) == (remote[fullBytes] & mask);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
