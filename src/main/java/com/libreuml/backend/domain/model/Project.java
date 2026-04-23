package com.libreuml.backend.domain.model;

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
    private long version;
    private Instant createdAt;
    private Instant updatedAt;

    public static Project create(UUID ownerId, String name, String description, String author,
                                  String projectVersion, String targetLanguage, String basePackage,
                                  ProjectKind projectKind) {
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
                .version(1L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void updateMetadata(String name, String description, String author,
                                String projectVersion, String targetLanguage, String basePackage,
                                UUID requesterId) {
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
        this.updatedAt = Instant.now();
    }

    public void assertOwner(UUID requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new ProjectOwnershipException("Only the project owner can perform this operation.");
        }
    }
}
