package com.libreuml.backend.infrastructure.in.web.dto.request.courseResource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.UUID;

public record UpdateCourseResourcePositionsRequest(
        @NotNull @Valid
        HashMap<UUID, Integer> idAndPositions
) {
}
