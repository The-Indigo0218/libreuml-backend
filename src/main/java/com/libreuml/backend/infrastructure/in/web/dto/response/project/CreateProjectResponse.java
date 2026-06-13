package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import java.time.Instant;
import java.util.UUID;

public record CreateProjectResponse(
        UUID id,
        UUID modelId,
        long version,
        Instant createdAt
) {}
