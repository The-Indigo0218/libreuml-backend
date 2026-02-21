package com.libreuml.backend.infrastructure.in.web.dto.response.courseResource;

import java.time.LocalDateTime;
import java.util.UUID;

public record CourseResourceResponse(
        UUID courseId,
        UUID resourceId,
        LocalDateTime createdAt,
        Integer position,
        Boolean visible
) {
}
