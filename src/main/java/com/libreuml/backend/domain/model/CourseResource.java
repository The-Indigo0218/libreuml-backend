package com.libreuml.backend.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class CourseResource {
    private UUID id;
    private UUID courseId;
    private UUID resourceId;
    private Integer position;
    private Boolean visible;
    private LocalDateTime createdAt;

    public void deactivate() { this.visible = false;}
}