package com.libreuml.backend.application.resource.port.in.dto;

import java.util.UUID;

public record DeactivateResourceCommand(UUID resourceId, UUID userId) {
}
