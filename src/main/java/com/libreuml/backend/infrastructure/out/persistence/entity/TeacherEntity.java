package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEntity extends UserEntity {

    @Column(name = "teacher_code")
    private String code;

    @Column(name = "student_count")
    private Integer studentCount;
}