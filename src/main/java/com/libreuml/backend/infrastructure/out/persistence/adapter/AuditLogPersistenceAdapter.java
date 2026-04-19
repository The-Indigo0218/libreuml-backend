package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.audit.port.out.AuditLogPort;
import com.libreuml.backend.domain.model.AuditLog;
import com.libreuml.backend.infrastructure.out.persistence.entity.AuditLogEntity;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogPort {

    private final SpringDataAuditLogRepository jpaRepository;

    @Override
    public void log(AuditLog entry) {
        try {
            jpaRepository.save(AuditLogEntity.builder()
                    .userId(entry.getUserId())
                    .eventType(entry.getEventType())
                    .ipAddress(entry.getIpAddress())
                    .userAgent(truncate(entry.getUserAgent(), 512))
                    .metadata(entry.getMetadata())
                    .createdAt(entry.getCreatedAt())
                    .build());
        } catch (Exception e) {
            log.error("Failed to persist audit log entry [{}]: {}", entry.getEventType(), e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
