package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.ResourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataResourceRepository extends JpaRepository<ResourceEntity, UUID> {

    @Query(value = "SELECT * FROM resources WHERE jsonb_exists(tags, :tag)",
            countQuery = "SELECT count(*) FROM resources WHERE jsonb_exists(tags, :tag)",
            nativeQuery = true)
    Page<ResourceEntity> findByTags(@Param("tag") String tag, Pageable pageable);

    Long countByActiveTrue();

    Page<ResourceEntity> findAllByCreatorId(UUID creatorId, Pageable pageable);

    Page<ResourceEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}