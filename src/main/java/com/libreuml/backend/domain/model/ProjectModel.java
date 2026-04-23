package com.libreuml.backend.domain.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectModel {

    private UUID id;
    private UUID projectId;
    private ObjectNode modelData;
    private long version;
    private Instant updatedAt;

    public static ProjectModel createEmpty(UUID projectId) {
        return ProjectModel.builder()
                .projectId(projectId)
                .modelData(null)
                .version(1L)
                .updatedAt(Instant.now())
                .build();
    }

    public void replaceData(ObjectNode modelData) {
        this.modelData = modelData;
        this.updatedAt = Instant.now();
    }
}
