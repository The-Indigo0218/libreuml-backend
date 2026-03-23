package com.libreuml.backend.infrastructure.in.web.dto.request.diagram;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.Size;

public record UpdateDiagramRequest(
        @Size(max = 255) String title,
        ObjectNode content,
        long version
) {}
