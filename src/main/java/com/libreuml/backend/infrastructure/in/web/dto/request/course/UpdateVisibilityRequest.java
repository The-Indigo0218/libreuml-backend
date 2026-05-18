package com.libreuml.backend.infrastructure.in.web.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVisibilityRequest(
        @NotBlank
        @Size(max = 20)
        String visibility
) {
}
