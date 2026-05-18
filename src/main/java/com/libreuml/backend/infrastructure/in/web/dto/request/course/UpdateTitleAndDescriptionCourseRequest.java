package com.libreuml.backend.infrastructure.in.web.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTitleAndDescriptionCourseRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description
) {
}
