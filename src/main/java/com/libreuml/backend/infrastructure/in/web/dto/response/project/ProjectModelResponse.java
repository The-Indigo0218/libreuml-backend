package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

public record ProjectModelResponse(
        UUID id,
        UUID projectId,
        ObjectNode data,
        long version,
        Instant updatedAt
) {}
