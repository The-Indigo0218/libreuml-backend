package com.libreuml.backend.domain.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.exception.DiagramOwnershipException;
import com.libreuml.backend.domain.model.exception.DiagramPayloadTooLargeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root for a UML diagram stored in the cloud.
 *
 * <p>All mutation invariants — ownership enforcement and the 5 MB payload ceiling —
 * are evaluated here, keeping the application service free of business logic.
 * The {@code content} field is represented as a Jackson {@link ObjectNode}: Jackson is a
 * data-structure library, not a framework, so its use in the domain does not introduce
 * an infrastructure dependency. The {@link ObjectNode} is the natural Java representation
 * of arbitrary structured JSON and does not carry any serialization annotations.
 *
 * <p>Identity is assigned by the JPA persistence layer on first save; the domain exposes
 * a nullable {@code id} until persistence completes.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diagram {

    private static final int MAX_CONTENT_BYTES = 5 * 1024 * 1024;

    private UUID id;
    private UUID ownerId;
    private String title;
    private DiagramType type;
    private DiagramVisibility visibility;
    private ObjectNode content;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private Set<UUID> collaboratorIds = new HashSet<>();

    public static Diagram create(UUID ownerId, String title, DiagramType type, ObjectNode content) {
        assertPayloadSize(content);
        return Diagram.builder()
                .ownerId(ownerId)
                .title(title)
                .type(type)
                .visibility(DiagramVisibility.PRIVATE)
                .content(content)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .collaboratorIds(new HashSet<>())
                .build();
    }

    /**
     * Applies a partial update. Null arguments indicate "leave unchanged."
     * Validates both ownership and the 5 MB ceiling before mutating state.
     */
    public void update(String title, ObjectNode content, UUID requesterId) {
        assertOwner(requesterId);
        if (content != null) {
            assertPayloadSize(content);
            this.content = content;
        }
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        this.updatedAt = Instant.now();
    }

    public void delete(UUID requesterId) {
        assertOwner(requesterId);
    }

    public boolean isAccessibleBy(UUID requesterId) {
        return ownerId.equals(requesterId)
                || visibility == DiagramVisibility.PUBLIC
                || (visibility == DiagramVisibility.SHARED && collaboratorIds.contains(requesterId));
    }

    private void assertOwner(UUID requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new DiagramOwnershipException("Only the diagram owner can perform this operation.");
        }
    }

    private static void assertPayloadSize(ObjectNode content) {
        if (content == null) return;
        int bytes = content.toString().getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_CONTENT_BYTES) {
            throw new DiagramPayloadTooLargeException(
                    "Diagram content exceeds the 5 MB limit (" + bytes + " bytes received).");
        }
    }
}
