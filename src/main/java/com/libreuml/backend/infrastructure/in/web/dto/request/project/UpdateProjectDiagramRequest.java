package com.libreuml.backend.infrastructure.in.web.dto.request.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectDiagramRequest(
        @Size(max = 255) String name,
        ObjectNode viewData,
        @NotNull Long version
) {}
