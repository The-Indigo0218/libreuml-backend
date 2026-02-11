package com.libreuml.backend.application.resource.port.in.dto;

import java.util.UUID;

public record UpdateTitleAndContentResourceCommand(UUID resourceId, String title, String content, UUID userId) {
}
