package com.libreuml.backend.infrastructure.in.web.dto.request.diagram;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDiagramRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull DiagramType type,
        ObjectNode content
) {}
