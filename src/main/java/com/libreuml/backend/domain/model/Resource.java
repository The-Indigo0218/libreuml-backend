package com.libreuml.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Resource {
    private UUID id;
    private String title;
    private ResourceType type;
    private String content;
    private UUID creatorId;
    private LocalDateTime createdAt;
    private Boolean active;
    private List<String> tags;

    public void deactivate() {
        this.active = false;
    }
}
