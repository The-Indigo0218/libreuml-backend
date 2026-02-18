package com.libreuml.backend.application.courseResource.port.in.dto;

import java.util.UUID;

public record DeactivateCourseResourceCommand(UUID id, UUID courseId, UUID userId) {
}
