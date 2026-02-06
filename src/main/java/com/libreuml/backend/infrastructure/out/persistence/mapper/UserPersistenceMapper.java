package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.infrastructure.out.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    default UserEntity toEntity(User user) {
        return switch (user) {
            case null -> null;
            case Student student -> toStudentEntity(student);
            case Teacher teacher -> toTeacherEntity(teacher);
            case Developer developer -> toDeveloperEntity(developer);
            default -> throw new IllegalArgumentException("unknow user type: " + user.getClass().getName());
        };
    }

    StudentEntity toStudentEntity(Student student);
    TeacherEntity toTeacherEntity(Teacher teacher);
    DeveloperEntity toDeveloperEntity(Developer developer);

    @SubclassMapping(source = StudentEntity.class, target = Student.class)
    @SubclassMapping(source = TeacherEntity.class, target = Teacher.class)
    @SubclassMapping(source = DeveloperEntity.class, target = Developer.class)
    User toDomain(UserEntity entity);

    Student toStudentDomain(StudentEntity entity);
    Teacher toTeacherDomain(TeacherEntity entity);
    Developer toDeveloperDomain(DeveloperEntity entity);

    SocialProfileEmbeddable toEmbeddable(SocialProfile profile);
    SocialProfile toSocialDomain(SocialProfileEmbeddable embeddable);
}