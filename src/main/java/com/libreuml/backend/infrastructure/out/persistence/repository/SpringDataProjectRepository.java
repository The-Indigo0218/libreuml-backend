package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    Page<ProjectEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProjectEntity p SET p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :projectId")
    void touchUpdatedAt(@Param("projectId") UUID projectId);
}
