package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.ApiDiagramType;

import java.time.Instant;
import java.util.UUID;

public record ProjectDiagramSummaryResponse(
        UUID id,
        String name,
        ApiDiagramType diagramType,
        String path,
        long version,
        Instant updatedAt
) {}
