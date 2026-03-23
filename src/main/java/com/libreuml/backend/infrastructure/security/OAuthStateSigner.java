package com.libreuml.backend.infrastructure.security;

import com.libreuml.backend.application.auth.exception.InvalidOAuthStateException;
import com.libreuml.backend.application.auth.port.out.OAuthStatePort;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Stateless HMAC-SHA256 implementation of {@link OAuthStatePort}.
 *
 * <p>State format: {@code BASE64URL(payload) "." BASE64URL(HMAC-SHA256(payload, secret))}
 * where {@code payload = nonce "|" epochSeconds}.
 *
 * <p>This approach requires no server-side storage (no cache, no DB) while still
 * providing replay-resistant, time-limited CSRF tokens.  The HMAC key is derived from
 * the same {@code JWT_SECRET} environment variable, so rotating that secret
 * simultaneously invalidates all outstanding state tokens — a deliberate property.
 */
@Component
public class OAuthStateSigner implements OAuthStatePort {

    private static final int STATE_VALIDITY_SECONDS = 15 * 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.secret}")
    private String jwtSecret;

    private byte[] signingKey;

    @PostConstruct
    void deriveKey() {
        signingKey = Base64.getDecoder().decode(jwtSecret);
    }

    @Override
    public String generateState() {
        byte[] nonceBytes = new byte[16];
        SECURE_RANDOM.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        String payload = nonce + "|" + Instant.now().getEpochSecond();
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + hmac(payload);
    }

    @Override
    public void validateState(String state) {
        String[] parts = state.split("\\.", 2);
        if (parts.length != 2) {
            throw new InvalidOAuthStateException("Malformed state parameter");
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidOAuthStateException("Invalid state encoding");
        }

        String expectedHmac = hmac(payload);
        // Constant-time comparison prevents timing-based signature oracle attacks.
        if (!MessageDigest.isEqual(
                expectedHmac.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidOAuthStateException("State signature verification failed");
        }

        String[] payloadParts = payload.split("\\|", 2);
        if (payloadParts.length != 2) {
            throw new InvalidOAuthStateException("Malformed state payload");
        }

        long issuedAt;
        try {
            issuedAt = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException e) {
            throw new InvalidOAuthStateException("Invalid state timestamp");
        }

        if (Instant.now().getEpochSecond() - issuedAt > STATE_VALIDITY_SECONDS) {
            throw new InvalidOAuthStateException("State parameter has expired");
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
