package com.libreuml.backend.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    private UUID id;
    private String code;
    private String name;
    private String description;

    private UUID teacherId;
    private LocalDateTime createdAt;
}