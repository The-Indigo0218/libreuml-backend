package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.DiagramVisibility;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String author,
        String projectVersion,
        String targetLanguage,
        String basePackage,
        DiagramVisibility visibility,
        long version,
        List<ProjectDiagramSummaryResponse> diagrams,
        Instant createdAt,
        Instant updatedAt
) {}
