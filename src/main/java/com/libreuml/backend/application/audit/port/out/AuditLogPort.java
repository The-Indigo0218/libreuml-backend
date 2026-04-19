package com.libreuml.backend.application.audit.port.out;

import com.libreuml.backend.domain.model.AuditLog;
import com.libreuml.backend.domain.model.AuditEventType;

import java.util.UUID;

public interface AuditLogPort {

    void log(AuditLog entry);

    default void log(AuditEventType eventType, UUID userId, String ipAddress, String userAgent) {
        log(AuditLog.builder()
                .eventType(eventType)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build());
    }

    default void log(AuditEventType eventType, UUID userId, String ipAddress, String userAgent, String metadata) {
        log(AuditLog.builder()
                .eventType(eventType)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata)
                .build());
    }
}
