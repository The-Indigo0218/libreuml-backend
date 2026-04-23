package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.ApiDiagramType;

import java.time.Instant;
import java.util.UUID;

public record ProjectDiagramListItemResponse(
        UUID id,
        UUID projectId,
        String name,
        ApiDiagramType diagramType,
        String path,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
