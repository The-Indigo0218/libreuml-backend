package com.libreuml.backend.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Answer {
    private UUID id;
    private String content;
    private Boolean isAccepted;
    private LocalDateTime createdAt;

    private UUID creatorId;
    private UUID questionId;
}