package com.libreuml.backend.application.courses.port.in.dto;

import java.util.UUID;

public record UpdateTitleAndDescriptionCourseCommand(UUID id, String title, String description, UUID userId) {
}
