package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.courseResource.port.in.dto.CreateCourseResourceCommand;
import com.libreuml.backend.application.courseResource.port.in.dto.UpdateCourseResourcePositionCommand;
import com.libreuml.backend.domain.model.CourseResource;
import com.libreuml.backend.infrastructure.in.web.dto.request.courseResource.CreateCourseResourceRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.courseResource.UpdateCourseResourcePositionsRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.courseResource.CourseResourceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseResourceWebMapper {

    @Mapping(target = "courseId", source = "request.courseId")
    @Mapping(target = "resourceId", source = "request.resourceId")
    @Mapping(target = "position", source = "request.position")
    @Mapping(target = "userId", source = "userId")
    CreateCourseResourceCommand toCreateCourseResourceCommand(CreateCourseResourceRequest request, UUID userId);

    @Mapping(target = "idAndPositions", source = "request.idAndPositions")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "userId", source = "userId")
    UpdateCourseResourcePositionCommand toUpdateCourseResourcePositionCommand(UpdateCourseResourcePositionsRequest request, UUID courseId, UUID userId);

    CourseResourceResponse toCourseResourceResponse(CourseResource courseResource);

    default PagedResult<CourseResourceResponse> toPagedCourseResourceResponse(PagedResult<CourseResource> result) {
        if (result == null) return null;

        var mappedContent = result.content().stream()
                .map(this::toCourseResourceResponse)
                .toList();

        return new PagedResult<>(
                mappedContent,
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
    }

}
