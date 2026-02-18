package com.libreuml.backend.application.courseResource.port.in;

import com.libreuml.backend.application.courseResource.port.in.dto.CreateCourseResourceCommand;
import com.libreuml.backend.domain.model.CourseResource;

public interface CreateCurseResourceUseCase {
    CourseResource create(CreateCourseResourceCommand command);
}