package com.libreuml.backend.infrastructure.in.web.controller;


import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courses.port.in.dto.*;
import com.libreuml.backend.application.courses.port.service.CourseService;
import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.CreateCourseRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.UpdateTitleAndDescriptionCourseRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.UpdateVisibilityRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.course.CourseResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.CourseWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final CourseWebMapper courseWebMapper;

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @RequestBody @Valid CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateCourseCommand command = courseWebMapper.toCreateCourseCommand(request, userDetails.getId());
        var course = courseService.createCourse(command);
        return ResponseEntity.ok(courseWebMapper.toCourseResponse(course));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<CourseResponse> updateTitleAndContent(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTitleAndDescriptionCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateTitleAndDescriptionCourseCommand command = courseWebMapper.toUpdateTitleAndDescriptionCourseCommand(request, id, userDetails.getId());
        var course = courseService.updateTitleAndDescription(command);
        return ResponseEntity.ok(courseWebMapper.toCourseResponse(course));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> toggleVisibility(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVisibilityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateCourseVisibilityCommand command = courseWebMapper.toUpdateCourseVisibilityCommand(request, id, userDetails.getId());
        courseService.updateVisibility(command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        courseService.deactivateCourse(new DeactivateCourseCommand(id, userDetails.getId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(courseWebMapper.toCourseResponse(courseService.getCourseById(id, userDetails.getId())));
    }

    @GetMapping
    public ResponseEntity<PagedResult<CourseResponse>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PaginationCommand pagination = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<Course> result = courseService.getAllPublicCourses(pagination);
        return ResponseEntity.ok(mapCoursePagedResult(result));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResult<CourseResponse>> search(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PaginationCommand pagination = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<Course> result = courseService.searchCursesByTitle(new SearchCoursesByTitleCommand(title, pagination));
        return ResponseEntity.ok(mapCoursePagedResult(result));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<PagedResult<CourseResponse>> listByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PaginationCommand pagination = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<Course> result = courseService.getCoursesByTag(new GetCourseByTag(tag, pagination));
        return ResponseEntity.ok(mapCoursePagedResult(result));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CourseResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(courseWebMapper.toCourseResponse(courseService.getCourseBySlug(slug)));
    }

    private PagedResult<CourseResponse> mapCoursePagedResult(PagedResult<Course> result) {
        List<CourseResponse> responses = result.content().stream()
                .map(courseWebMapper::toCourseResponse)
                .toList();
        return new PagedResult<>(responses, result.pageNumber(), result.pageSize(),
                result.totalElements(), result.totalPages(), result.isLast());
    }
}