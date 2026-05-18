package com.libreuml.backend.infrastructure.in.web.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCourseRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 20)
        String visibility,

        @Size(max = 50)
        String code,

        @Size(max = 10)
        List<String> tags
) {}