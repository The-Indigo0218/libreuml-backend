package com.libreuml.backend.application.courses.port.in.dto;

import com.libreuml.backend.application.common.dto.PaginationCommand;

import java.util.UUID;

public record GetCoursesByCreatorIdCommand(UUID creatorId, PaginationCommand paginationCommand) {
}
