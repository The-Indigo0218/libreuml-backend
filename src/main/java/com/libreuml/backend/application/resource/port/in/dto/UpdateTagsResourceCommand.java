package com.libreuml.backend.application.resource.port.in.dto;

import java.util.List;
import java.util.UUID;

public record UpdateTagsResourceCommand(UUID resourceId, List<String> tags, UUID userId) {
}
