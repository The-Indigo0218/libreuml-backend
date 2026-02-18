package com.libreuml.backend.infrastructure.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_resources", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_id", "resource_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    @Column(nullable = false)
    private Integer position;

    @Builder.Default
    private Boolean visible = true;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime createdAt;
}