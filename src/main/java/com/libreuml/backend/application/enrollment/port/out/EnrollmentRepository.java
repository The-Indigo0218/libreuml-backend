package com.libreuml.backend.application.enrollment.port.out;

import com.libreuml.backend.domain.model.Enrollment;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository {
    Enrollment save(Enrollment enrollment);

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    PagedResult<Enrollment> findAllByCourseId(UUID courseId, PaginationCommand pagination);

    PagedResult<Enrollment> findAllByStudentId(UUID studentId, PaginationCommand pagination);
}