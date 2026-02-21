package com.libreuml.backend.application.courseResource.port.in;

import com.libreuml.backend.application.courseResource.port.in.dto.CourseIdAndResourceIdCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.DeactivateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.UpdateCourseResourcePositionCommand;

import java.util.UUID;


public interface UpdateCourseResourceUseCase {
    void updatePosition(UpdateCourseResourcePositionCommand command);
    void deactivateCourseResource(DeactivateCourseResourceCommand command);
}