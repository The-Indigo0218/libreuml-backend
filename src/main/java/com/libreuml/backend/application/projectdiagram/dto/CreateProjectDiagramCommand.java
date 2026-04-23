package com.libreuml.backend.application.projectdiagram.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ApiDiagramType;

import java.util.UUID;

public record CreateProjectDiagramCommand(
        UUID projectId,
        UUID requesterId,
        String name,
        ApiDiagramType diagramType,
        String path,
        ObjectNode viewData
) {}
