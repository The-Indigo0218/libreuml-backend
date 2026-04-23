package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ApiDiagramType;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.ProjectKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectFullResponse(
        ProjectData project,
        ModelData model,
        List<DiagramData> diagrams
) {
    public record ProjectData(
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
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ModelData(
            UUID id,
            ObjectNode data,
            long version,
            Instant updatedAt
    ) {}

    public record DiagramData(
            UUID id,
            String name,
            ApiDiagramType diagramType,
            String path,
            ObjectNode viewData,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
