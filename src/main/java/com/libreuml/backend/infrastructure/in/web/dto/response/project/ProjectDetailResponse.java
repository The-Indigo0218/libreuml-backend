package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.ProjectKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectDetailResponse(
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
        ObjectNode vfsSnapshot,
        List<ProjectDiagramSummaryResponse> diagrams,
        Instant createdAt,
        Instant updatedAt
) {}
