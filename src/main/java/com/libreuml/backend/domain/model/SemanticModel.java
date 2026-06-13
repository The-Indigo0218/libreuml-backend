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
 * The semantic model of a project: the single source of truth for its UML/C4 elements,
 * stored as an opaque JSON document ({@code data}). One-to-one with {@link Project} — the
 * {@code projectId} both identifies the owning project and serves as the access boundary, so
 * this aggregate carries no owner of its own; ownership is checked against the parent project.
 *
 * <p>An empty model is created together with its project (see {@code CreateProjectUseCase}); the
 * frontend then fills it via {@code PATCH /projects/{id}/model}. Optimistic locking uses the
 * {@code version} counter, matching the {@link Diagram} convention.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticModel {

    private static final int MAX_DATA_BYTES = 5 * 1024 * 1024;

    private UUID id;
    private UUID projectId;
    private ObjectNode data;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    /** Creates the empty model that is persisted alongside a brand-new project. */
    public static SemanticModel createEmpty(UUID projectId, ObjectNode emptyData) {
        return SemanticModel.builder()
                .projectId(projectId)
                .data(emptyData)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /** Replaces the model payload. Validates the 5 MB ceiling before mutating state. */
    public void updateData(ObjectNode data) {
        if (data != null) {
            assertDataSize(data);
            this.data = data;
        }
        this.updatedAt = Instant.now();
    }

    private static void assertDataSize(ObjectNode data) {
        if (data == null) return;
        int bytes = data.toString().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_DATA_BYTES) {
            throw new ProjectPayloadTooLargeException(
                    "Semantic model data exceeds the 5 MB limit (" + bytes + " bytes received).");
        }
    }
}
