package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataProjectModelRepository extends JpaRepository<ProjectModelEntity, UUID> {

    Optional<ProjectModelEntity> findByProjectId(UUID projectId);

    @Query(nativeQuery = true,
            value = "SELECT COALESCE(pg_column_size(model_data), 0) FROM project_models WHERE project_id = :projectId")
    long getModelDataBytesByProjectId(@Param("projectId") UUID projectId);

    @Query(nativeQuery = true,
            value = "SELECT COALESCE(SUM(pg_column_size(pm.model_data)), 0) " +
                    "FROM project_models pm JOIN projects p ON pm.project_id = p.id " +
                    "WHERE p.owner_id = :userId")
    long getTotalModelDataBytesByOwner(@Param("userId") UUID userId);
}
