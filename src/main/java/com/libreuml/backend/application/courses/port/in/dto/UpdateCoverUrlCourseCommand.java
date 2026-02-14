package com.libreuml.backend.application.courses.port.in.dto;

import java.util.UUID;

public record UpdateCoverUrlCourseCommand(UUID id, String coverUrl, UUID userId) {
}
