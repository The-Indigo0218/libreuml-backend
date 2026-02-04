package com.libreuml.backend.application.courses.port.in;

import com.libreuml.backend.application.courses.port.in.dto.CreateCourseCommand;
import com.libreuml.backend.domain.model.Course;

public interface CreateCourseUseCase {
    Course createCourse(CreateCourseCommand command);
}
