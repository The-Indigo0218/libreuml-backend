package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.CourseResource;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseResourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseResourcePersistenceMapper {

    @Mapping(target = "courseId", source = "course.getId")
    @Mapping(target = "resourceId", source = "resource.getId")
    CourseResource toDomain(CourseResourceEntity entity);

    @Mapping(target = "course.id", source = "courseId")
    @Mapping(target = "resource.id", source = "resourceId")
    CourseResourceEntity toEntity(CourseResource courseResource);
}
