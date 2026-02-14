package com.libreuml.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Enrollment {

    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private boolean active;
    private LocalDateTime enrolledAt;

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}