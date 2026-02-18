package com.libreuml.backend.application.courseResource.port.in.dto;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public record UpdateCourseResourcePositionCommand(HashMap<UUID, Integer> idAndPositions, UUID courseId, UUID userId) {}