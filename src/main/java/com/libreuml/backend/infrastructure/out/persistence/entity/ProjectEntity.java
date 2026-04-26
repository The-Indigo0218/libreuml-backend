package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.ProjectKind;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String author;

    @Column(name = "project_version", nullable = false, length = 50)
    private String projectVersion;

    @Column(name = "target_language", length = 50)
    private String targetLanguage;

    @Column(name = "base_package", length = 255)
    private String basePackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiagramVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_kind", nullable = true)
    private ProjectKind projectKind;

    @Column(name = "vfs_snapshot", columnDefinition = "jsonb")
    private ObjectNode vfsSnapshot;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
