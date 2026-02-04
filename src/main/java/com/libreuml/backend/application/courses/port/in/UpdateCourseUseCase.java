package com.libreuml.backend.application.courses.port.in;

import com.libreuml.backend.application.courses.port.in.dto.DeactivateCourseCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateCourseVisibilityCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateCoverUrlCourseCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateTitleAndDescriptionCourseCommand;
import com.libreuml.backend.domain.model.Course;

import java.util.UUID;

public interface UpdateCourseUseCase {

    Course updateTitleAndDescription(UpdateTitleAndDescriptionCourseCommand command);

    Course updateCoverUrl(UpdateCoverUrlCourseCommand command);

    Course updateVisibility(UpdateCourseVisibilityCommand command);

    Course deactivateCourse(DeactivateCourseCommand command);
}
