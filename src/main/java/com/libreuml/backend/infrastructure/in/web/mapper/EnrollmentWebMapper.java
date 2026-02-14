package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.enrollment.port.in.dto.EnrollmentCommand;
import com.libreuml.backend.domain.model.Enrollment;
import com.libreuml.backend.infrastructure.in.web.dto.response.enrollment.EnrollmentResponse;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface EnrollmentWebMapper {

    EnrollmentResponse toResponse(Enrollment enrollment);

    EnrollmentCommand toCommand(UUID studentId, UUID courseId);
}