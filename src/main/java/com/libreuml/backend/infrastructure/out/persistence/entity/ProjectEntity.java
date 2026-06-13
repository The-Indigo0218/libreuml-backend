package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.ProjectKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 255)
    private String author;

    /** User-facing semantic label (e.g. "1.0.0"); unrelated to the {@code version} lock counter. */
    @Column(name = "project_version", nullable = false, length = 50)
    private String projectVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_kind", nullable = false, length = 50)
    private ProjectKind projectKind;

    @Column(name = "target_language", length = 100)
    private String targetLanguage;

    @Column(name = "base_package", length = 255)
    private String basePackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiagramVisibility visibility;

    /** VFS tree structure stored as PostgreSQL {@code jsonb}. See {@link DiagramEntity#getContent()}. */
    @JdbcTypeCode(SqlTypes.JSON)
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
