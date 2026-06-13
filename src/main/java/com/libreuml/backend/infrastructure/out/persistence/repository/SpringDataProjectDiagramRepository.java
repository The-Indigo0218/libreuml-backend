package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectDiagramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SpringDataProjectDiagramRepository extends JpaRepository<ProjectDiagramEntity, UUID> {
    List<ProjectDiagramEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    /** (projectId, diagramType) pairs for the given projects; aggregated into counts by the adapter. */
    @Query("SELECT d.projectId, d.diagramType FROM ProjectDiagramEntity d WHERE d.projectId IN :ids")
    List<Object[]> findProjectIdAndType(@Param("ids") Collection<UUID> ids);
}
