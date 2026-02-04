package com.libreuml.backend.application.courses.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Course;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository {
    Course save(Course course);
    Optional<Course> findById(UUID id);

    boolean existsByCode(String code);

    PagedResult<Course> findAllByCreatorId(UUID creatorId, PaginationCommand pagination);

    PagedResult<Course> findAllPublicCourses(PaginationCommand pagination);

    PagedResult<Course> searchByTitle(String title, PaginationCommand pagination);

    long countAll();
    long countAllPublic();
}
