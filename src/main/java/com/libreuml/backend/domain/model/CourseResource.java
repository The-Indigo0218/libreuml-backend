package com.libreuml.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
public class CourseResource {
    private UUID id;
    private UUID courseId;
    private UUID resourceId;
    private Integer position;
    private Boolean visible;
    private LocalDateTime createdAt;

    public void deactivate() { this.visible = false;}
}