package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.CreateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.DeactivateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.UpdateCourseResourcePositionCommand;
import com.libreuml.backend.application.courseResource.port.service.CourseResourceService;
import com.libreuml.backend.domain.model.CourseResource;
import com.libreuml.backend.infrastructure.in.web.dto.request.courseResource.CreateCourseResourceRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.courseResource.UpdateCourseResourcePositionsRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.courseResource.CourseResourceResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.CourseResourceWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/course-resources")
public class CourseResourceController {

    private final CourseResourceService courseResourceService;
    private final CourseResourceWebMapper courseResourceWebMapper;

    @PostMapping()
    public ResponseEntity<CourseResourceResponse> createCourseResource(
            @RequestBody @Valid CreateCourseResourceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        CreateCourseResourceCommand command = courseResourceWebMapper.toCreateCourseResourceCommand(request, userDetails.getId());
        CourseResource courseResource = courseResourceService.create(command);
        return ResponseEntity.ok(courseResourceWebMapper.toCourseResourceResponse(courseResource));
    }

    @PutMapping("/{courseId}/updatePositions")
    public ResponseEntity<Void> updateCourseResourcePositions(
            @PathVariable @Valid UUID courseId,
            @RequestBody @Valid UpdateCourseResourcePositionsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        UpdateCourseResourcePositionCommand command = courseResourceWebMapper.toUpdateCourseResourcePositionCommand(request, courseId, userDetails.getId());
        courseResourceService.updatePosition(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResourceResponse> getCourseResourceById(
            @PathVariable @Valid UUID id
    ){
        CourseResource courseResource = courseResourceService.getCourseResourceById(id);
        return ResponseEntity.ok(courseResourceWebMapper.toCourseResourceResponse(courseResource));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<PagedResult<CourseResourceResponse>> getCourseResourceByCourseId(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "position") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @PathVariable @Valid UUID courseId
    ){
        PaginationCommand pagination = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<CourseResource> pagedCourseResources = courseResourceService.getAllCourseResourcesByCourseId(courseId, pagination);
        PagedResult<CourseResourceResponse> response = courseResourceWebMapper.toPagedCourseResourceResponse(pagedCourseResources);
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{courseId}/{id}")
    public ResponseEntity<Void> deactivateCourseResource(
            @PathVariable @Valid UUID courseId,
            @PathVariable @Valid UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DeactivateCourseResourceCommand command = new DeactivateCourseResourceCommand(id, courseId, userDetails.getId());
        courseResourceService.deactivateCourseResource(command);
        return ResponseEntity.noContent().build();
    }

}
