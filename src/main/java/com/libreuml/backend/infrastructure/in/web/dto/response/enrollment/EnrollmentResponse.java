package com.libreuml.backend.infrastructure.in.web.dto.response.enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID studentId,
        UUID courseId,
        boolean active,
        LocalDateTime enrolledAt
) {}