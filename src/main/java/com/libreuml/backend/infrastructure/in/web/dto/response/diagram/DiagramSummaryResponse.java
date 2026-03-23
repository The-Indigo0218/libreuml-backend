package com.libreuml.backend.infrastructure.in.web.dto.response.diagram;

import com.libreuml.backend.domain.model.DiagramType;
import com.libreuml.backend.domain.model.DiagramVisibility;

import java.time.Instant;
import java.util.UUID;

public record DiagramSummaryResponse(
        UUID id,
        String title,
        DiagramType type,
        DiagramVisibility visibility,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
