package com.libreuml.backend.application.courses.port.in.dto;

import java.util.List;
import java.util.UUID;

public record UpdateCourseTagsCommand(List<String> tags, UUID id, UUID creatorId) {}