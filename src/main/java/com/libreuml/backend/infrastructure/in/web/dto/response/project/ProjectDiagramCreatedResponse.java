package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectDiagramCreatedResponse(UUID id, UUID projectId, long version, Instant createdAt) {}
