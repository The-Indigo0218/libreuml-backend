package com.libreuml.backend.application.courseResource.port.in.dto;

import java.util.UUID;

public record CourseIdAndResourceIdCommand(UUID courseId, UUID resourceId) {}