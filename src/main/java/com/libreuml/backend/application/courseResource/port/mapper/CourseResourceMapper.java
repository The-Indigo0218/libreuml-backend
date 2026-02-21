package com.libreuml.backend.application.courseResource.port.mapper;

import com.libreuml.backend.application.courseResource.port.in.dto.CreateCourseResourceCommand;
import com.libreuml.backend.domain.model.CourseResource;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseResourceMapper {

    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "position", source = "position")
    @Mapping(target = "id", ignore = true, expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "visible", constant = "true")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    CourseResource toCourseResource(CreateCourseResourceCommand command);

    @Mapping(target = "position", source = "position")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePosition(Integer position, @MappingTarget CourseResource courseResource);

}
