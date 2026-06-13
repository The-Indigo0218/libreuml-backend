package com.libreuml.backend.application.project.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record UpdateProjectDiagramCommand(
        UUID projectId,
        UUID diagramId,
        UUID requesterId,
        String name,
        ObjectNode viewData,
        long version
) {}
