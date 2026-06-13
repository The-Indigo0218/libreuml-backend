package com.libreuml.backend.application.project.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ProjectDiagramType;

import java.util.UUID;

public record CreateProjectDiagramCommand(
        UUID projectId,
        UUID requesterId,
        String name,
        ProjectDiagramType diagramType,
        String path,
        ObjectNode viewData
) {}
