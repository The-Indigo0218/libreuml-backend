package com.libreuml.backend.infrastructure.in.web.dto.request.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ApiDiagramType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectDiagramRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ApiDiagramType diagramType,
        @Size(max = 500) String path,
        ObjectNode viewData
) {}
