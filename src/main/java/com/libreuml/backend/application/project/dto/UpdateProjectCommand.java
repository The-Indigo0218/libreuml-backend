package com.libreuml.backend.application.project.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

public record UpdateProjectCommand(
        UUID projectId,
        UUID requesterId,
        String name,
        String description,
        String author,
        String projectVersion,
        String targetLanguage,
        String basePackage,
        ObjectNode vfsSnapshot,
        long version
) {}
