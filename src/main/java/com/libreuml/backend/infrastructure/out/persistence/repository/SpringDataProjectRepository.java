package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProjectRepository extends JpaRepository<ProjectEntity, UUID> {
    Page<ProjectEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
