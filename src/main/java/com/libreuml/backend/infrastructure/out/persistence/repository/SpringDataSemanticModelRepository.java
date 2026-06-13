package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.SemanticModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataSemanticModelRepository extends JpaRepository<SemanticModelEntity, UUID> {
    Optional<SemanticModelEntity> findByProjectId(UUID projectId);
}
