package com.libreuml.backend.infrastructure.in.web.dto.request.course;

import jakarta.validation.constraints.NotNull;

public record UpdateTitleAndDescriptionCourseRequest(@NotNull String title, @NotNull String description) {
}
