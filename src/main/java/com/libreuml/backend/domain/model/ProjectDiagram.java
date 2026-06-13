package com.libreuml.backend.domain.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.exception.ProjectPayloadTooLargeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * A diagram that belongs to a {@link Project}. Distinct from the standalone {@link Diagram}
 * aggregate: it carries {@code name}/{@code viewData}/{@code path} instead of
 * {@code title}/{@code content}, uses the extended {@link ProjectDiagramType}, and has no
 * visibility or collaborators of its own — those live on the parent project.
 *
 * <p>It holds no owner: access is decided against the owning project, reached via {@code projectId}.
 * {@code path} stores the VFS node UUID the frontend assigns, so a reloaded project can re-attach
 * each diagram to its place in the file tree.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDiagram {

    private static final int MAX_VIEW_DATA_BYTES = 5 * 1024 * 1024;

    private UUID id;
    private UUID projectId;
    private String name;
    private ProjectDiagramType diagramType;
    private String path;                 // VFS node UUID assigned by the frontend
    private ObjectNode viewData;         // canvas nodes/edges (+ optional embedded local model), jsonb
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProjectDiagram create(UUID projectId, String name, ProjectDiagramType diagramType,
                                         String path, ObjectNode viewData) {
        assertViewDataSize(viewData);
        return ProjectDiagram.builder()
                .projectId(projectId)
                .name(name)
                .diagramType(diagramType != null ? diagramType : ProjectDiagramType.UNSPECIFIED)
                .path(path)
                .viewData(viewData)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Applies a partial update. Null arguments mean "leave unchanged."
     * The frontend's update contract only carries {@code name} and {@code viewData};
     * {@code diagramType} and {@code path} are fixed once the diagram is created.
     */
    public void update(String name, ObjectNode viewData) {
        if (viewData != null) {
            assertViewDataSize(viewData);
            this.viewData = viewData;
        }
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.updatedAt = Instant.now();
    }

    private static void assertViewDataSize(ObjectNode viewData) {
        if (viewData == null) return;
        int bytes = viewData.toString().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_VIEW_DATA_BYTES) {
            throw new ProjectPayloadTooLargeException(
                    "Diagram view data exceeds the 5 MB limit (" + bytes + " bytes received).");
        }
    }
}
