package com.libreuml.backend.application.project.dto;

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
        long version
) {}
