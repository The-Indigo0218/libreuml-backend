package com.libreuml.backend.application.courses.port.in.dto;

import com.libreuml.backend.domain.model.VisibilityCourseEnum;

import java.util.UUID;

public record UpdateCourseVisibilityCommand(UUID courseId, UUID userId, VisibilityCourseEnum visibility) {
}
