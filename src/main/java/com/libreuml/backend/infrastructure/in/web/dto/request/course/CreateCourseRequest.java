package com.libreuml.backend.infrastructure.in.web.dto.request.course;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCourseRequest(
        @NotNull
        String title,
        String description,
        @NotNull
        String visibility,
        String code,
        List<String> tags
) {}