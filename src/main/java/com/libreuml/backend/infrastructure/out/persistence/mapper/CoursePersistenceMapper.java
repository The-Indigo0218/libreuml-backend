package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.Course;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseEntity;
import com.libreuml.backend.infrastructure.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CoursePersistenceMapper {

    @Mapping(source = "creator.id", target = "creatorId")
    Course toDomain(CourseEntity entity);

    @Mapping(source = "creatorId", target = "creator", qualifiedByName = "userIdToUserEntity")
    CourseEntity toEntity(Course domain);

    @Named("userIdToUserEntity")
    default UserEntity map(UUID userId) {
        if (userId == null) return null;
        return UserEntity.builder().id(userId).build();
    }
}