package com.libreuml.backend.application.enrollment.port.in.dto;

import java.util.UUID;

public record EnrollmentCommand(UUID studentId, UUID courseId) {}

