package com.libreuml.backend.application.project.dto;

import com.libreuml.backend.domain.model.ProjectKind;

import java.util.UUID;

public record CreateProjectCommand(
        UUID ownerId,
        String name,
        String description,
        String author,
        String projectVersion,
        String targetLanguage,
        String basePackage,
        ProjectKind projectKind
) {}
