package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.Report;
import com.libreuml.backend.infrastructure.out.persistence.entity.ReportEntity;
import com.libreuml.backend.infrastructure.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportPersistenceMapper {

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUserEntity")
    ReportEntity toEntity(Report domain);

    @Mapping(source = "user.id", target = "userId")
    Report toDomain(ReportEntity entity);

    @Named("userIdToUserEntity")
    default UserEntity map(UUID userId) {
        if (userId == null) return null;
        return UserEntity.builder().id(userId).build();
    }

}
