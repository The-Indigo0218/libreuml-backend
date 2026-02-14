package com.libreuml.backend.application.courses.port.mapper;

import com.libreuml.backend.application.courses.port.in.dto.*;
import com.libreuml.backend.domain.model.Course;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseMapper {

    @Mapping(target = "code", source = "code")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "coverUrl", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "visibility", source = "visibility")
    @Mapping(target = "tags", source = "tags")
    Course toDomain(CreateCourseCommand command);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTitleAndDescriptionFromCommand(UpdateTitleAndDescriptionCourseCommand command, @MappingTarget Course course);

    @Mapping(target = "coverUrl", source = "coverUrl")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCoverUrlFromCommand(UpdateCoverUrlCourseCommand command, @MappingTarget Course course);

    @Mapping(target = "visibility", source = "visibility")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVisibilityFromCommand(UpdateCourseVisibilityCommand command, @MappingTarget Course course);

    @Mapping(target = "tags", source = "tags")
    @Mapping(target = "id", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTagsFromCommand(UpdateCourseTagsCommand command, @MappingTarget Course course);
}