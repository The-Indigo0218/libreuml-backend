package com.libreuml.backend.infrastructure.in.web.dto.request.resource;

import com.libreuml.backend.domain.model.ResourceType;

import java.util.UUID;

public record CreateResourceRequest(String title, String type, String content, UUID creatorId) {}