package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectDiagramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProjectDiagramRepository extends JpaRepository<ProjectDiagramEntity, UUID> {

    List<ProjectDiagramEntity> findAllByProjectId(UUID projectId);

    Optional<ProjectDiagramEntity> findByProjectIdAndId(UUID projectId, UUID id);

    @Query(nativeQuery = true,
            value = "SELECT project_id::text, diagram_type, COUNT(*) FROM diagrams " +
                    "WHERE project_id IN :projectIds GROUP BY project_id, diagram_type")
    List<Object[]> getDiagramTypeCountsByProjectIds(@Param("projectIds") Collection<UUID> projectIds);

    @Query(nativeQuery = true,
            value = "SELECT COALESCE(SUM(pg_column_size(d.view_data)), 0) " +
                    "FROM diagrams d JOIN projects p ON d.project_id = p.id " +
                    "WHERE p.owner_id = :userId")
    long getTotalViewDataBytesByOwner(@Param("userId") UUID userId);
}
