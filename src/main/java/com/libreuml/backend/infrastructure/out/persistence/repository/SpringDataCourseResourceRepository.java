package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.CourseResourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCourseResourceRepository extends JpaRepository<CourseResourceEntity, UUID> {

    Page<CourseResourceEntity> findAllByCourseId(UUID courseId, Pageable pageable);

}
