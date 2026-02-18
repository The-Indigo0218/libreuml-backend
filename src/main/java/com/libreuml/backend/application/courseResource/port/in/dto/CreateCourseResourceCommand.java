package com.libreuml.backend.application.courseResource.port.in.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateCourseResourceCommand(UUID courseId, UUID resourceId, Integer position, LocalDateTime createdAt, UUID userId) {
}
