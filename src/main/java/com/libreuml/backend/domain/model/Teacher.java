package com.libreuml.backend.domain.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
public class Teacher extends User {
    private String code;
    private Integer studentCount;
}