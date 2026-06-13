package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.ProjectDiagramType;
import com.libreuml.backend.domain.model.ProjectKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID id,
        String name,
        String description,
        String author,
        String projectVersion,
        ProjectKind projectKind,
        String targetLanguage,
        String basePackage,
        DiagramVisibility visibility,
        long version,
        int diagramCount,
        List<ProjectDiagramType> diagramTypes,
        Instant createdAt,
        Instant updatedAt
) {}
