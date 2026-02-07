package com.libreuml.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Resource {
    UUID id;
    String title;
    ResourceType type;
    String content;
    UUID creatorId;
    private LocalDateTime createdAt;
    private Boolean active;
}
