package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectDiagramEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataProjectDiagramRepository extends JpaRepository<ProjectDiagramEntity, UUID> {
    List<ProjectDiagramEntity> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
