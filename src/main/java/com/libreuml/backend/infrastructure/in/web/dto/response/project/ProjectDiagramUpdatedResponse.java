package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectDiagramUpdatedResponse(UUID id, long version, Instant updatedAt) {}
