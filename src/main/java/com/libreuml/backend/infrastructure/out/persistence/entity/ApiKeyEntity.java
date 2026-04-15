package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.libreuml.backend.domain.model.ApiKey;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(name = "hashed_key", nullable = false, unique = true, length = 64)
    private String hashedKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private ApiKey.KeyType keyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApiKey.Scope scope;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "partner_email")
    private String partnerEmail;

    @Column(name = "rate_limit_read", nullable = false)
    private int rateLimitRead;

    @Column(name = "rate_limit_write", nullable = false)
    private int rateLimitWrite;

    @Column(name = "redemption_code", length = 50)
    private String redemptionCode;

    @Column(name = "redemption_limit")
    private Integer redemptionLimit;

    @Column(name = "redemption_count", nullable = false)
    private int redemptionCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
