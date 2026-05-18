package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.infrastructure.out.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Builder;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserPersistenceMapper {

    default UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        return switch (user) {
            case Student student -> toStudentEntity(student);
            case Teacher teacher -> toTeacherEntity(teacher);
            case Developer developer -> toDeveloperEntity(developer);
            default -> throw new IllegalArgumentException("Unknown user type: " + user.getClass().getName());
        };
    }

    StudentEntity toStudentEntity(Student student);
    TeacherEntity toTeacherEntity(Teacher teacher);
    DeveloperEntity toDeveloperEntity(Developer developer);

  default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        // Unwrap Hibernate proxy to get actual type
        if (entity.getClass().getName().contains("HibernateProxy")) {
            var unwrap = org.hibernate.Hibernate.unproxy(entity);
            if (unwrap instanceof UserEntity unwrappedEntity) {
                entity = unwrappedEntity;
            }
        }

        if (entity instanceof StudentEntity s) {
            return toStudentDomain(s);
        } else if (entity instanceof TeacherEntity t) {
            return toTeacherDomain(t);
        } else if (entity instanceof DeveloperEntity d) {
            return toDeveloperDomain(d);
        }
        throw new IllegalArgumentException("Unknown entity type: " + entity.getClass().getName());
    }

    Student toStudentDomain(StudentEntity entity);
    Teacher toTeacherDomain(TeacherEntity entity);
    Developer toDeveloperDomain(DeveloperEntity entity);

    // --- Helpers ---
    SocialProfileEmbeddable toEmbeddable(SocialProfile profile);
    SocialProfile toSocialDomain(SocialProfileEmbeddable embeddable);
}