package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.CourseResource;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseEntity;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseResourceEntity;
import com.libreuml.backend.infrastructure.out.persistence.entity.ResourceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseResourcePersistenceMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "resourceId", source = "resource.id")
    @Mapping(target = "id", ignore = true)
    CourseResource toDomain(CourseResourceEntity entity);

    @Mapping(target = "course", source = "courseId", qualifiedByName = "courseIdToCourseEntity")
    @Mapping(target = "resource", source = "resourceId", qualifiedByName = "resourceIdToResourceEntity")
    CourseResourceEntity toEntity(CourseResource courseResource);

    @Named("courseIdToCourseEntity")
    default CourseEntity mapCourseIdToCourseEntity(UUID courseId) {
        if (courseId == null) return null;
        return CourseEntity.builder().id(courseId).build();
    }

    @Named("resourceIdToResourceEntity")
    default ResourceEntity mapResourceIdToResourceEntity(UUID resourceId) {
        if (resourceId == null) return null;
        return ResourceEntity.builder().id(resourceId).build();
    }
}
