package com.libreuml.backend.infrastructure.in.web.dto.request.courseResource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCourseResourceRequest(
        @Valid @NotNull
        Integer position,
        @Valid @NotNull
        UUID resourceId,
        @Valid @NotNull
        UUID courseId
) {
}
