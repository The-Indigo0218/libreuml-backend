package com.libreuml.backend.domain.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
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
public class ProjectDiagram {

    private UUID id;
    private UUID projectId;
    private String name;
    private ApiDiagramType diagramType;
    private String path;
    private ObjectNode viewData;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProjectDiagram create(UUID projectId, String name, ApiDiagramType diagramType,
                                         String path, ObjectNode viewData) {
        return ProjectDiagram.builder()
                .projectId(projectId)
                .name(name)
                .diagramType(diagramType)
                .path(path)
                .viewData(viewData)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void update(String name, ObjectNode viewData) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (viewData != null) {
            this.viewData = viewData;
        }
        this.updatedAt = Instant.now();
    }
}
