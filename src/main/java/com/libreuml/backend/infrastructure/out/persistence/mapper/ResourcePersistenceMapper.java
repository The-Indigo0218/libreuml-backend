package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.Resource;
import com.libreuml.backend.infrastructure.out.persistence.entity.ResourceEntity;
import com.libreuml.backend.infrastructure.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ResourcePersistenceMapper {

    @Mapping(source = "creator.id", target = "creatorId")
    Resource toDomain(ResourceEntity entity);

    @Mapping(source = "creatorId", target = "creator", qualifiedByName = "userIdToUserEntity")
    ResourceEntity toEntity(Resource domain);

    @Named("userIdToUserEntity")
    default UserEntity map(UUID userId) {
        if (userId == null) return null;
        return UserEntity.builder().id(userId).build();
    }
}