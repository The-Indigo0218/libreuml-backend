package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.DiagramEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDiagramRepository extends JpaRepository<DiagramEntity, UUID> {
    List<DiagramEntity> findByOwnerId(UUID ownerId);
}
