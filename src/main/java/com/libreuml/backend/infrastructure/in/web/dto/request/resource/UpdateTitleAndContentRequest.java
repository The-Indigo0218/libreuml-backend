package com.libreuml.backend.infrastructure.in.web.dto.request.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleAndContentRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 100000)
        String content
) {
}
