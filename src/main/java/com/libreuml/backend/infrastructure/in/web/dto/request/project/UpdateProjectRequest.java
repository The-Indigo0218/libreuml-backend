package com.libreuml.backend.infrastructure.in.web.dto.request.project;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255) String name,
        String description,
        @Size(max = 255) String author,
        @Size(max = 50) String projectVersion,
        @Size(max = 50) String targetLanguage,
        @Size(max = 255) String basePackage,
        ObjectNode vfsSnapshot,
        @NotNull Long version
) {}
