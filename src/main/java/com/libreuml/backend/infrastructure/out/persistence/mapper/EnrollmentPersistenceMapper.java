package com.libreuml.backend.infrastructure.out.persistence.mapper;

import com.libreuml.backend.domain.model.Enrollment;
import com.libreuml.backend.infrastructure.out.persistence.entity.EnrollmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentPersistenceMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "courseId", source = "course.id")
    Enrollment toDomain(EnrollmentEntity entity);

    @Mapping(target = "student.id", source = "studentId")
    @Mapping(target = "course.id", source = "courseId")
    EnrollmentEntity toEntity(Enrollment enrollment);
}