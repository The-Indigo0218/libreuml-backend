package com.libreuml.backend.application.courseResource.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.CourseResource;

import java.util.Optional;
import java.util.UUID;

public interface GetCourseResourceUseCase {
    PagedResult<CourseResource> getAllCourseResourcesByCourseId(UUID courseId, PaginationCommand command);
    CourseResource getCourseResourceById(UUID id);
}