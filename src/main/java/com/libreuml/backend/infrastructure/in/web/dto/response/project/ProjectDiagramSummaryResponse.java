package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.ProjectDiagramType;

import java.time.Instant;
import java.util.UUID;

/** Lightweight diagram view embedded in {@link ProjectDetailResponse} (no viewData payload). */
public record ProjectDiagramSummaryResponse(
        UUID id,
        String name,
        ProjectDiagramType diagramType,
        String path,
        long version,
        Instant updatedAt
) {}
