package com.libreuml.backend.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Question {
    private UUID id;
    private String title;
    private String content;
    private List<String> tags;
    private Boolean active;
    private LocalDateTime createdAt;
    private UUID creatorId;

    public void deactivate() {
        this.active = false;
    }
}