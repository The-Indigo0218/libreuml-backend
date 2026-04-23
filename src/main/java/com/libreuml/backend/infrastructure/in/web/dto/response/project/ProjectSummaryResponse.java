package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.libreuml.backend.domain.model.ApiDiagramType;
import com.libreuml.backend.domain.model.DiagramVisibility;
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
        String targetLanguage,
        String basePackage,
        DiagramVisibility visibility,
        ProjectKind projectKind,
        long version,
        long diagramCount,
        List<ApiDiagramType> diagramTypes,
        Instant createdAt,
        Instant updatedAt
) {}
