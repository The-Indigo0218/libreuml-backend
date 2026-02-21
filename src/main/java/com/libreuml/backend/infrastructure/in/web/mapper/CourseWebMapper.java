package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.courses.port.in.dto.CreateCourseCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateCourseVisibilityCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateTitleAndDescriptionCourseCommand;
import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.domain.model.VisibilityCourseEnum;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.CreateCourseRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.UpdateTitleAndDescriptionCourseRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.course.UpdateVisibilityRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.course.CourseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseWebMapper {

    @Mapping(target = "visibility", source = "request.visibility", qualifiedByName = "validateVisibility")
    CreateCourseCommand toCreateCourseCommand(CreateCourseRequest request, UUID creatorId);

    UpdateTitleAndDescriptionCourseCommand toUpdateTitleAndDescriptionCourseCommand(UpdateTitleAndDescriptionCourseRequest request, UUID id, UUID creatorId);

    @Mapping(target = "visibility", source = "request.visibility", qualifiedByName = "validateVisibility")
    UpdateCourseVisibilityCommand toUpdateCourseVisibilityCommand(UpdateVisibilityRequest request, UUID id, UUID creatorId);

    CourseResponse toCourseResponse(Course course);

    @Named("validateVisibility")
    default VisibilityCourseEnum validateVisibility(String visibility) {
        try {
            return VisibilityCourseEnum.valueOf(visibility.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid visibility value: " + visibility);
        }
    }

}
