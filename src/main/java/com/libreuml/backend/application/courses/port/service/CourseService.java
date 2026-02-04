package com.libreuml.backend.application.courses.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.exception.CourseAlreadyExistsException;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.courses.port.in.*;
import com.libreuml.backend.application.courses.port.in.dto.*;
import com.libreuml.backend.application.courses.port.mapper.CourseMapper;
import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.domain.model.RoleEnum;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService implements CreateCourseUseCase, GetCourseUseCase, UpdateCourseUseCase {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;


    @Override
    public Course createCourse(CreateCourseCommand command) {
        User user = getUserOrThrow(command.creatorId());

        if (!user.getRole().equals(RoleEnum.TEACHER) && !user.getRole().equals(RoleEnum.ADMIN)) {
            throw new UserNotAuthorizedException("Only Teachers and Admins can create courses.");
        }

        if (courseRepository.existsByCode(command.code())) {
            throw new CourseAlreadyExistsException("Course with code " + command.code() + " already exists.");
        }
        Course course = courseMapper.toDomain(command);
        return courseRepository.save(course);
    }

    @Override
    public Course getCourseById(UUID courseId) {
        return getCourseOrThrow(courseId);
    }

    @Override
    public PagedResult<Course> getAllCoursesByCreatorId(GetCoursesByCreatorIdCommand command) {
         getUserOrThrow(command.creatorId());
        return courseRepository.findAllByCreatorId(command.creatorId(), command.paginationCommand());
    }

    @Override
    public PagedResult<Course> getAllPublicCourses(PaginationCommand paginationCommand) {
        return courseRepository.findAllPublicCourses(paginationCommand);
    }

    @Override
    public Long getTotalCoursesCount() {
        return courseRepository.countAll();
    }

    @Override
    public Long getTotalPublicCoursesCount() {
        return courseRepository.countAllPublic();
    }

    @Override
    public PagedResult<Course> searchCursesByTitle(SearchCoursesByTitleCommand command) {
        return courseRepository.searchByTitle(command.title(), command.paginationCommand());
    }

    @Override
    public Course updateTitleAndDescription(UpdateTitleAndDescriptionCourseCommand command) {
        getUserOrThrow(command.userId());
        Course course = getCourseOrThrow(command.courseId());
        verifyCourseCreator(course, command.userId());
        courseMapper.updateTitleAndDescriptionFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course updateCoverUrl(UpdateCoverUrlCourseCommand command) {
        getUserOrThrow(command.userId());
        Course course = getCourseOrThrow(command.courseId());
        verifyCourseCreator(course, command.userId());
        courseMapper.updateCoverUrlFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course updateVisibility(UpdateCourseVisibilityCommand command) {
        getUserOrThrow(command.userId());
        Course course = getCourseOrThrow(command.courseId());
        verifyCourseCreator(course, command.userId());
        courseMapper.updateVisibilityFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course deactivateCourse(DeactivateCourseCommand command) {
        Course course = getCourseOrThrow(command.courseId());
        User user = getUserOrThrow(command.userId());
        course.deactivate(user);
        return courseRepository.save(course);
    }

    private Course getCourseOrThrow(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException("Course with id " + courseId + " not found."));
    }

    private User getUserOrThrow(UUID userId) {
      return userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found."));
    }

    private void verifyCourseCreator(Course course, UUID userId) {
        if (!course.getCreatorId().equals(userId)) {
            throw new UserNotAuthorizedException("User is not authorized to perform this action.");
        }
    }
}
