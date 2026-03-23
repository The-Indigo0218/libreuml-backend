package com.libreuml.backend.infrastructure.out.persistence.entity;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.domain.model.DiagramType;
import com.libreuml.backend.domain.model.DiagramVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "diagrams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiagramType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiagramVisibility visibility;

    /**
     * The diagram's JSON payload stored as PostgreSQL {@code jsonb}.
     * Hibernate 6's native {@code SqlTypes.JSON} support serializes {@link ObjectNode} via Jackson
     * and communicates the value to the PostgreSQL JDBC driver as a typed JSON parameter,
     * avoiding the VARCHAR→JSONB cast rejection that an {@code AttributeConverter<ObjectNode,String>}
     * would cause.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private ObjectNode content;

    /**
     * Optimistic locking counter managed exclusively by JPA.  On every successful
     * {@code UPDATE}, Hibernate appends {@code WHERE version = :current} to the statement and
     * increments the column atomically.  If two concurrent sessions attempt to flush
     * simultaneously, the second one finds 0 rows affected and JPA throws
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}.
     */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "diagram_collaborators",
            joinColumns = @JoinColumn(name = "diagram_id")
    )
    @Column(name = "user_id")
    @Builder.Default
    private Set<UUID> collaboratorIds = new HashSet<>();
}
