package com.libreuml.backend.application.courses.port.in.dto;

import com.libreuml.backend.domain.model.VisibilityCourseEnum;

import java.util.UUID;

public record CreateCourseCommand( String title, String description, UUID creatorId, VisibilityCourseEnum visibility, String code) {
}
