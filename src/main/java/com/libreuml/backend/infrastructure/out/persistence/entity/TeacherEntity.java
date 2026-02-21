package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TeacherEntity extends UserEntity {

    @Column(name = "teacher_code")
    private String code;

    @Column(name = "student_count")
    private Integer studentCount;
}