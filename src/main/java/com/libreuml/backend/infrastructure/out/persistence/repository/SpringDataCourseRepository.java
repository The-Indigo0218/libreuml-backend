package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.domain.model.VisibilityCourseEnum;
import com.libreuml.backend.infrastructure.out.persistence.entity.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCourseRepository extends JpaRepository<CourseEntity, UUID> {

    long countByVisibility(VisibilityCourseEnum visibility);

    boolean existsBySlug(String slug);
    boolean existsByCode(String code);

    Optional<CourseEntity> findBySlug(String slug);

    Page<CourseEntity> findAllByCreatorId(UUID creatorId, Pageable pageable);

    Page<CourseEntity> findAllByVisibility(VisibilityCourseEnum visibility, Pageable pageable);

    Page<CourseEntity> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query(value = "SELECT * FROM courses WHERE jsonb_exists(tags, :tag)",
            countQuery = "SELECT count(*) FROM courses WHERE jsonb_exists(tags, :tag)",
            nativeQuery = true)
    Page<CourseEntity> findByTags(@Param("tag") String tag, Pageable pageable);
}