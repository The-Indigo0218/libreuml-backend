package com.libreuml.backend.application.courses.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.port.in.dto.GetCourseByTag;
import com.libreuml.backend.application.courses.port.in.dto.GetCoursesByCreatorIdCommand;
import com.libreuml.backend.application.courses.port.in.dto.SearchCoursesByTitleCommand;
import com.libreuml.backend.domain.model.Course;

import java.util.UUID;

public interface GetCourseUseCase {

    Course getCourseById(UUID courseId);

    PagedResult<Course> getAllCoursesByCreatorId(GetCoursesByCreatorIdCommand command);

    PagedResult<Course> getAllPublicCourses(PaginationCommand paginationCommand);

    Long getTotalCoursesCount();

    Long getTotalPublicCoursesCount();

    PagedResult<Course> searchCursesByTitle(SearchCoursesByTitleCommand command);

    PagedResult<Course> getCoursesByTag(GetCourseByTag command);

    Course getCourseBySlug(String slug);
}
