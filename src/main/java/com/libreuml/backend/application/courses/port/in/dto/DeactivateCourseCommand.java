package com.libreuml.backend.application.courses.port.in.dto;

import java.util.UUID;

public record DeactivateCourseCommand(UUID id, UUID userId) {
}
