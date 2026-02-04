package com.libreuml.backend.application.courses.port.mapper;

import com.libreuml.backend.application.courses.port.in.dto.CreateCourseCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateCourseVisibilityCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateCoverUrlCourseCommand;
import com.libreuml.backend.application.courses.port.in.dto.UpdateTitleAndDescriptionCourseCommand;
import com.libreuml.backend.domain.model.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

    @Mapping(target = "code", source = "code")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "coverUrl", source = "coverUrl")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "visibility", source = "visibility")
    Course toDomain(CreateCourseCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    void updateTitleAndDescriptionFromCommand(UpdateTitleAndDescriptionCourseCommand command, @MappingTarget Course course);

    @Mapping(target = "coverUrl", source = "coverUrl")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    void  updateCoverUrlFromCommand(UpdateCoverUrlCourseCommand command, @MappingTarget Course course);

    @Mapping(target = "visibility", source = "visibility")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    void updateVisibilityFromCommand(UpdateCourseVisibilityCommand command, @MappingTarget Course course);

}
