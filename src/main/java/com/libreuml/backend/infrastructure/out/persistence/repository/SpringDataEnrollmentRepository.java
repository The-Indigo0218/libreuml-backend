package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.infrastructure.out.persistence.entity.EnrollmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Optional<EnrollmentEntity> findByStudentIdAndCourseId(UUID studentId, UUID courseId);

    Page<EnrollmentEntity> findAllByCourseId(UUID courseId, Pageable pageable);

    Page<EnrollmentEntity> findAllByStudentId(UUID studentId, Pageable pageable);
}