package com.libreuml.backend.infrastructure.in.web.dto.response.diagram;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramType;
import com.libreuml.backend.domain.model.DiagramVisibility;

import java.time.Instant;
import java.util.UUID;

public record DiagramResponse(
        UUID id,
        UUID ownerId,
        String title,
        DiagramType type,
        DiagramVisibility visibility,
        ObjectNode content,
        long version,
        Instant createdAt,
        Instant updatedAt
) {}
