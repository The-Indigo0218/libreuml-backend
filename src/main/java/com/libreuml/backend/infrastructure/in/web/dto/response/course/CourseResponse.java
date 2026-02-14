package com.libreuml.backend.infrastructure.in.web.dto.response.course;

import com.libreuml.backend.domain.model.VisibilityCourseEnum;

import java.util.List;
import java.util.UUID;

public record CourseResponse(String title, String description, String code, String coverUrl, List<String> tags, VisibilityCourseEnum visibility) {}