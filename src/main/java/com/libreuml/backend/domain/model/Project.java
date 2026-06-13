package com.libreuml.backend.domain.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.ProjectPayloadTooLargeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for a project: the project-centric unit the modeler frontend syncs to the cloud.
 *
 * <p>A project owns exactly one {@link SemanticModel} and zero-or-more {@link ProjectDiagram}s.
 * Those are separate aggregates persisted in their own tables; the project references them by id,
 * it does not hold them in-memory. Ownership of the whole graph is decided here: the model and the
 * diagrams have no owner of their own and are reached only through their parent project.
 *
 * <p>Mirrors the conventions of {@link Diagram}: ownership and the 5 MB payload ceiling are domain
 * invariants enforced in this class, {@code content}-like JSON is a Jackson {@link ObjectNode}, and
 * identity is assigned by the persistence layer on first save.
 *
 * <p><b>Two distinct "version" fields — do not conflate:</b>
 * <ul>
 *   <li>{@code projectVersion} — a user-edited semantic label such as {@code "1.0.0"}. Plain data.</li>
 *   <li>{@code version} — the optimistic-locking counter managed by JPA {@code @Version}. Concurrency.</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    private static final int MAX_SNAPSHOT_BYTES = 5 * 1024 * 1024;

    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private String author;
    private String projectVersion;        // semantic label, e.g. "1.0.0" — NOT the @Version counter
    private ProjectKind projectKind;
    private String targetLanguage;
    private String basePackage;
    private DiagramVisibility visibility;
    private ObjectNode vfsSnapshot;       // VFS tree structure, stored as jsonb
    private long version;                 // optimistic-lock counter (JPA @Version)
    private Instant createdAt;
    private Instant updatedAt;

    public static Project create(UUID ownerId, String name, String description, String author,
                                 String projectVersion, ProjectKind projectKind,
                                 String targetLanguage, String basePackage, ObjectNode vfsSnapshot) {
        assertSnapshotSize(vfsSnapshot);
        return Project.builder()
                .ownerId(ownerId)
                .name(name)
                .description(description)
                .author(author)
                .projectVersion(projectVersion != null && !projectVersion.isBlank() ? projectVersion : "1.0.0")
                .projectKind(projectKind != null ? projectKind : ProjectKind.FREE)
                .targetLanguage(targetLanguage)
                .basePackage(basePackage)
                .visibility(DiagramVisibility.PRIVATE)
                .vfsSnapshot(vfsSnapshot)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Applies a partial update. Null arguments mean "leave unchanged."
     * Validates ownership and the 5 MB snapshot ceiling before mutating state.
     */
    public void update(String name, String description, String author,
                       String targetLanguage, String basePackage,
                       ObjectNode vfsSnapshot, UUID requesterId) {
        assertOwner(requesterId);
        if (vfsSnapshot != null) {
            assertSnapshotSize(vfsSnapshot);
            this.vfsSnapshot = vfsSnapshot;
        }
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null)             this.description = description;
        if (author != null)                  this.author = author;
        if (targetLanguage != null)          this.targetLanguage = targetLanguage;
        if (basePackage != null)             this.basePackage = basePackage;
        this.updatedAt = Instant.now();
    }

    public void delete(UUID requesterId) {
        assertOwner(requesterId);
    }

    public boolean isAccessibleBy(UUID requesterId) {
        return ownerId.equals(requesterId) || visibility == DiagramVisibility.PUBLIC;
    }

    private void assertOwner(UUID requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new ProjectOwnershipException("Only the project owner can perform this operation.");
        }
    }

    private static void assertSnapshotSize(ObjectNode vfsSnapshot) {
        if (vfsSnapshot == null) return;
        int bytes = vfsSnapshot.toString().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_SNAPSHOT_BYTES) {
            throw new ProjectPayloadTooLargeException(
                    "Project VFS snapshot exceeds the 5 MB limit (" + bytes + " bytes received).");
        }
    }
}
