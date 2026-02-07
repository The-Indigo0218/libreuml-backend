package com.libreuml.backend.application.courses.port.in.dto;

import com.libreuml.backend.application.common.dto.PaginationCommand;

public record GetCourseByTag(String tag, PaginationCommand paginationCommand) {
}
