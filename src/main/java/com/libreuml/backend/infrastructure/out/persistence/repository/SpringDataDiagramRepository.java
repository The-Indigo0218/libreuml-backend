package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.DiagramEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDiagramRepository extends JpaRepository<DiagramEntity, UUID> {
    Page<DiagramEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
