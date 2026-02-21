package com.libreuml.backend.infrastructure.out.persistence.repository;

import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportStatus;
import com.libreuml.backend.domain.model.ReportType;
import com.libreuml.backend.infrastructure.out.persistence.entity.ReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataReportRepository extends JpaRepository<ReportEntity, UUID> {
    Page<ReportEntity> findByUserId(UUID userId, Pageable paginationCommand);
    Page<ReportEntity> findByStatus(ReportStatus status, Pageable pageable);
    Page<ReportEntity> findByPriority(ReportPriority priority, Pageable pageable);
    Page<ReportEntity> findByType(ReportType type, Pageable pageable);
    Page<ReportEntity> findByTypeAndPriorityAndStatus(ReportType type, ReportPriority priority, ReportStatus status, Pageable paginationCommand);
}
