package com.libreuml.backend.application.diagram.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record UpdateDiagramCommand(
        UUID diagramId,
        UUID requesterId,
        String title,
        ObjectNode content,
        long version
) {}
