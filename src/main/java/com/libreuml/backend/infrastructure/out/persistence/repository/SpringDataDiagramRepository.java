package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.infrastructure.out.persistence.entity.DiagramEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDiagramRepository extends JpaRepository<DiagramEntity, UUID> {
    List<DiagramEntity> findByOwnerId(UUID ownerId);
    Page<DiagramEntity> findByVisibility(DiagramVisibility visibility, Pageable pageable);
}
