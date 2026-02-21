package com.libreuml.backend.infrastructure.in.web.dto.response.resource;

import com.libreuml.backend.domain.model.ResourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String title,
        String content,
        ResourceType type,
        List<String> tags,
        Boolean active,
        LocalDateTime createdAt,
        UUID creatorId
) {}