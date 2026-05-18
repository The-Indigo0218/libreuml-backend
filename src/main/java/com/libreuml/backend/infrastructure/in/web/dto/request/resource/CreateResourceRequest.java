package com.libreuml.backend.infrastructure.in.web.dto.request.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateResourceRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 50)
        String type,

        @Size(max = 100000)
        String content,

        UUID creatorId
) {}