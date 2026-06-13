package com.libreuml.backend.infrastructure.in.web.dto.request.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.ProjectKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 255) String author,
        @Size(max = 50) String projectVersion,
        ProjectKind projectKind,
        @Size(max = 100) String targetLanguage,
        @Size(max = 255) String basePackage,
        ObjectNode vfsSnapshot
) {}
