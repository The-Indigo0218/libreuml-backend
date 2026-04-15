package com.libreuml.backend.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root representing an API key issued for external or MCP access.
 *
 * <p>Two key types are supported:
 * <ul>
 *   <li>{@link KeyType#USER} — self-service keys created by users in Settings.</li>
 *   <li>{@link KeyType#PARTNER} — admin-managed keys, optionally distributable via
 *       redemption codes to partner organisations.</li>
 * </ul>
 *
 * <p>Plain-text key values are never stored; only a SHA-256 hex digest is persisted.
 * The first 15 characters of the full key are stored as {@code keyPrefix} for display
 * in list endpoints.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    public enum KeyType { USER, PARTNER }

    public enum Scope { READ, WRITE }

    private UUID id;
    private UUID userId;
    private String name;
    private String keyPrefix;
    private String hashedKey;
    private KeyType keyType;
    private Scope scope;

    private String partnerName;
    private String partnerEmail;

    @Builder.Default private int rateLimitRead  = 20;
    @Builder.Default private int rateLimitWrite = 10;

    private String  redemptionCode;
    private Integer redemptionLimit;
    @Builder.Default private int redemptionCount = 0;

    private Instant createdAt;
    private UUID    createdBy;
    private Instant lastUsedAt;

    @Builder.Default private long    usageCount = 0L;
    @Builder.Default private boolean active     = true;

    private Instant revokedAt;
    private UUID    revokedBy;
    private String  notes;

    /**
     * Returns true when the key has not been revoked and its {@code active} flag is set.
     */
    public boolean isActive() {
        return active && revokedAt == null;
    }

    /**
     * Returns true when the key's scope permits the given HTTP method.
     *
     * <p>READ scope only allows {@code GET}; WRITE scope allows any method.
     */
    public boolean canPerform(String httpMethod) {
        if (scope == Scope.READ) {
            return "GET".equalsIgnoreCase(httpMethod);
        }
        return true;
    }

    /**
     * Atomically marks this key as revoked. The actual DB persistence is handled
     * by the repository; this method keeps the domain state consistent.
     */
    public void revoke(UUID adminId) {
        this.active    = false;
        this.revokedAt = Instant.now();
        this.revokedBy = adminId;
    }

    /**
     * Increments the redemption counter when a user redeems a partner code.
     * The returned key is a new USER key created for the redeemer, not the
     * partner key itself — this method only tracks how many times the code
     * has been used.
     */
    public void incrementRedemptionCount() {
        this.redemptionCount++;
    }

    /**
     * Returns true if this key still has available redemption slots, or if
     * no limit has been set (null redemption_limit = unlimited).
     */
    public boolean canBeRedeemed() {
        return isActive()
               && (redemptionLimit == null || redemptionCount < redemptionLimit);
    }
}
