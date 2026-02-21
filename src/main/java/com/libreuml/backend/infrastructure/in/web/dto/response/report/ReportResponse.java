package com.libreuml.backend.infrastructure.in.web.dto.response.report;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        String title,
        String description,
        String status,
        String priority,
        String adminResponse,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
        ) {
}
