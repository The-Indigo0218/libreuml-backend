package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportStatus;
import com.libreuml.backend.domain.model.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ReportEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private ReportPriority priority;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT", name = "admin_response")
    private String adminResponse;

    @Column(columnDefinition = "TEXT", name = "internal_notes")
    private String internalNotes;

    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    private LocalDateTime solvedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "evidences_images")
    private Set<String> evidencesImages;
}
