package com.libreuml.backend.infrastructure.in.web.dto.request.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;

public record UpdateModelRequest(
        @NotNull ObjectNode data,
        long version
) {}
