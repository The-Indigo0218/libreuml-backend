package com.libreuml.backend.application.enrollment.port.in;

import com.libreuml.backend.application.enrollment.port.in.dto.EnrollmentCommand;
import com.libreuml.backend.domain.model.Enrollment;


public interface ManageEnrollmentUseCase {

    void joinCourse(EnrollmentCommand command);

    void leaveCourse(EnrollmentCommand command);

    Enrollment getEnrollmentDetails(EnrollmentCommand command);
}