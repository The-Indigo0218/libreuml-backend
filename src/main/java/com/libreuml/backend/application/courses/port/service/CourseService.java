package com.libreuml.backend.application.courses.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.exception.CourseAlreadyExistsException;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.courses.port.in.*;
import com.libreuml.backend.application.courses.port.in.dto.*;
import com.libreuml.backend.application.courses.port.mapper.CourseMapper;
import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.application.enrollment.port.out.EnrollmentRepository;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService implements CreateCourseUseCase, GetCourseUseCase, UpdateCourseUseCase {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final EnrollmentRepository enrollmentRepository;


    @Override
    @Transactional
    public Course createCourse(CreateCourseCommand command) {
        if (command.tags().size() > 5) {
            throw new IllegalArgumentException("A course cannot have more than 5 tags.");
        }
        User user = getUserOrThrow(command.creatorId());
        if (user.getRole() == RoleEnum.STUDENT) {
            throw new UserNotAuthorizedException("You are not authorized to create a course.");
        }
        if (courseRepository.existsByCode(command.code())) {
            throw new CourseAlreadyExistsException("Course with code " + command.code() + " already exists.");
        }
        Course course = courseMapper.toDomain(command);
        course.assignSlug(generateUniqueSlug(command.title()));
        return courseRepository.save(course);
    }

    @Override
    public Course getCourseById(UUID courseId, UUID userId) {
        Course course = getCourseOrThrow(courseId);
        if (course.getVisibility().equals(VisibilityCourseEnum.PRIVATE)){
            verifyCourseMember(course, userId);
        }
        return course;
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
    public PagedResult<Course> getCoursesByTag(GetCourseByTag command) {
        return courseRepository.findByTag(command.tag(), command.paginationCommand());
    }

    @Override
    public Course getCourseBySlug(String slug) {
        return courseRepository.findBySlug(slug).orElseThrow(() -> new CourseNotFoundException("Course with slug " + slug + " not found."));
    }

    @Override
    public Course updateTitleAndDescription(UpdateTitleAndDescriptionCourseCommand command) {
        getUserOrThrow(command.creatorId());
        Course course = getCourseOrThrow(command.id());
        verifyCourseCreator(course, command.creatorId());
        courseMapper.updateTitleAndDescriptionFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course updateCoverUrl(UpdateCoverUrlCourseCommand command) {
        getUserOrThrow(command.creatorId());
        Course course = getCourseOrThrow(command.id());
        verifyCourseCreator(course, command.creatorId());
        courseMapper.updateCoverUrlFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course updateVisibility(UpdateCourseVisibilityCommand command) {
        getUserOrThrow(command.creatorId());
        Course course = getCourseOrThrow(command.id());
        verifyCourseCreator(course, command.creatorId());
        courseMapper.updateVisibilityFromCommand(command, course);
        return courseRepository.save(course);
    }

    @Override
    public Course deactivateCourse(DeactivateCourseCommand command) {
        Course course = getCourseOrThrow(command.id());
        User user = getUserOrThrow(command.userId());
        course.deactivate(user);
        return courseRepository.save(course);
    }

    @Override
    public Course updateTags(UpdateCourseTagsCommand command) {
        Course course = getCourseOrThrow(command.id());
        verifyCourseCreator(course, command.creatorId());
        courseMapper.updateTagsFromCommand(command, course);
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

    private String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");

        String finalSlug = baseSlug;
        int counter = 1;

        while (courseRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + counter;
            counter++;
        }

        return finalSlug;
    }

    private void verifyCourseMember(Course course, UUID userId) {
        if (!course.getCreatorId().equals(userId) && !enrollmentRepository.existsByStudentIdAndCourseId(userId, course.getId())) {
            throw new UserNotAuthorizedException("User is not authorized to perform this action.");
        }
    }

}
