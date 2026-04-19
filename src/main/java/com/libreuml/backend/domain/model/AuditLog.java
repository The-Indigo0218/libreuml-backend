package com.libreuml.backend.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AuditLog {

    private UUID id;
    private UUID userId;
    private AuditEventType eventType;
    private String ipAddress;
    private String userAgent;
    private String metadata;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
