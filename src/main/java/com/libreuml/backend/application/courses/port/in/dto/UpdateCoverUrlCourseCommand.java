package com.libreuml.backend.application.courses.port.in.dto;

import java.util.UUID;

public record UpdateCoverUrlCourseCommand(UUID courseId, String coverUrl, UUID userId) {
}
