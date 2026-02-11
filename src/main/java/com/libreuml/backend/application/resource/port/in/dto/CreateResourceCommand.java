package com.libreuml.backend.application.resource.port.in.dto;

import com.libreuml.backend.domain.model.ResourceType;

import java.util.UUID;

public record CreateResourceCommand(String title, ResourceType type, String content, UUID creatorId) {
}