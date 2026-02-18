package com.libreuml.backend.application.courseResource.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courseResource.exception.CourseResourceNotFound;
import com.libreuml.backend.application.courseResource.port.in.CreateCurseResourceUseCase;
import com.libreuml.backend.application.courseResource.port.in.GetCourseResourceUseCase;
import com.libreuml.backend.application.courseResource.port.in.UpdateCourseResourceUseCase;
import com.libreuml.backend.application.courseResource.port.in.dto.CreateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.DeactivateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.UpdateCourseResourcePositionCommand;
import com.libreuml.backend.application.courseResource.port.mapper.CourseResourceMapper;
import com.libreuml.backend.application.courseResource.port.out.CourseResourceRepository;
import com.libreuml.backend.application.courses.exception.CourseNotFoundException;
import com.libreuml.backend.application.courses.port.out.CourseRepository;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.resource.exception.ResourceNotFoundException;
import com.libreuml.backend.application.resource.port.out.ResourceRepository;
import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.domain.model.CourseResource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseResourceService implements CreateCurseResourceUseCase, UpdateCourseResourceUseCase, GetCourseResourceUseCase {

    private final CourseResourceRepository courseResourceRepository;
    private final CourseRepository courseRepository;
    private final ResourceRepository resourceRepository;
    private final CourseResourceMapper courseResourceMapper;

    @Override
    public CourseResource create(CreateCourseResourceCommand command) {
        verifyOwnership(command.courseId(), command.userId());
        findCourseOrThrow(command.courseId());
        findResourceOrThrow(command.resourceId());
        CourseResource courseResource = courseResourceMapper.toCourseResource(command);
        return courseResourceRepository.save(courseResource);
    }

    @Override
    public PagedResult<CourseResource> getAllCourseResourcesByCourseId(UUID courseId, PaginationCommand command) {
        findCourseOrThrow(courseId);
        return courseResourceRepository.getAllCourseResourcesByCourseId(courseId, command);
    }

    @Override
    public CourseResource getCourseResourceById(UUID id) {
        return getCourseResourceByIdOrThrow(id);
    }

    @Override
    public void updatePosition(UpdateCourseResourcePositionCommand command) {
        verifyOwnership(command.courseId(), command.userId());
        command.idAndPositions().forEach((id, position) -> {
            CourseResource courseResource = getCourseResourceByIdOrThrow(id);
            verifyResourceBelongsToCourse(courseResource, command.courseId());
            courseResourceMapper.updatePosition(position, courseResource);
            courseResourceRepository.save(courseResource);
        });
    }

    @Override
    public void deactivateCourseResource(DeactivateCourseResourceCommand command) {
        verifyOwnership(command.courseId(), command.userId());
        CourseResource courseResource = getCourseResourceByIdOrThrow(command.id());
        verifyResourceBelongsToCourse(courseResource, command.courseId());
        courseResource.deactivate();
        courseResourceRepository.save(courseResource);
    }

    public CourseResource getCourseResourceByIdOrThrow(UUID id) {
        return courseResourceRepository.findById(id).orElseThrow(() -> new CourseResourceNotFound("Course resource with id " + id + " not found"));
    }

    private Course findCourseOrThrow(UUID courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new CourseNotFoundException("Course with id " + courseId + " not found"));
    }

    private void findResourceOrThrow(UUID resourceId) {
        resourceRepository.findById(resourceId).orElseThrow(() -> new ResourceNotFoundException("Resource with id " + resourceId + " not found"));
    }

    private void verifyResourceBelongsToCourse(CourseResource courseResource, UUID courseId) {
        if (!courseResource.getCourseId().equals(courseId)) {
            throw new CourseResourceNotFound("Course resource with id " + courseResource.getId() + " not found in course with id " + courseId);
        }
    }

    private void verifyOwnership(UUID courseId, UUID userId) {
        Course course = findCourseOrThrow(courseId);
        if (!course.getCreatorId().equals(userId)) {
            throw new UserNotAuthorizedException("User with id " + userId + " is not authorized to perform this action on course with id " + courseId);
        }
    }

}

