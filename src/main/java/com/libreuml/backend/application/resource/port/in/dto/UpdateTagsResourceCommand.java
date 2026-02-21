package com.libreuml.backend.application.resource.port.in.dto;

import java.util.List;
import java.util.UUID;

public record UpdateTagsResourceCommand(UUID id, List<String> tags, UUID creatorId) {
}
