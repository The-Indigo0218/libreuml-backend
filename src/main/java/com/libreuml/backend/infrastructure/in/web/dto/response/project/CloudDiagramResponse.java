package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ProjectDiagramType;

import java.time.Instant;
import java.util.UUID;

public record CloudDiagramResponse(
        UUID id,
        UUID projectId,
        String name,
        ProjectDiagramType diagramType,
        String path,
        ObjectNode viewData,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
