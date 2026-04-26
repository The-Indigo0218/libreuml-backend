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
public class Project {

    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private String author;
    private String projectVersion;
    private String targetLanguage;
    private String basePackage;
    private DiagramVisibility visibility;
    private ProjectKind projectKind;
    private ObjectNode vfsSnapshot;
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static Project create(UUID ownerId, String name, String description, String author,
                                  String projectVersion, String targetLanguage, String basePackage,
                                  ProjectKind projectKind, ObjectNode vfsSnapshot) {
        return Project.builder()
                .ownerId(ownerId)
                .name(name)
                .description(description)
                .author(author)
                .projectVersion(projectVersion != null ? projectVersion : "1.0.0")
                .targetLanguage(targetLanguage)
                .basePackage(basePackage)
                .visibility(DiagramVisibility.PRIVATE)
                .projectKind(projectKind != null ? projectKind : ProjectKind.FREE)
                .vfsSnapshot(vfsSnapshot)
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void updateMetadata(String name, String description, String author,
                                String projectVersion, String targetLanguage, String basePackage,
                                ObjectNode vfsSnapshot, UUID requesterId) {
        assertOwner(requesterId);
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (author != null) {
            this.author = author;
        }
        if (projectVersion != null && !projectVersion.isBlank()) {
            this.projectVersion = projectVersion;
        }
        if (targetLanguage != null) {
            this.targetLanguage = targetLanguage;
        }
        if (basePackage != null) {
            this.basePackage = basePackage;
        }
        if (vfsSnapshot != null) {
            this.vfsSnapshot = vfsSnapshot;
        }
        this.updatedAt = Instant.now();
    }

    public void assertOwner(UUID requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new ProjectOwnershipException("Only the project owner can perform this operation.");
        }
    }
}
