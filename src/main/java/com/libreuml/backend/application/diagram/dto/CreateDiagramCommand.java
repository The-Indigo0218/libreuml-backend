package com.libreuml.backend.application.diagram.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramType;

import java.util.UUID;

public record CreateDiagramCommand(
        UUID ownerId,
        String title,
        DiagramType type,
        ObjectNode content
) {}
