package com.libreuml.backend.infrastructure.out.persistence.adapter;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.report.port.out.ReportRepository;
import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.infrastructure.out.persistence.entity.ReportEntity;
import com.libreuml.backend.infrastructure.out.persistence.mapper.ReportPersistenceMapper;
import com.libreuml.backend.infrastructure.out.persistence.repository.SpringDataReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final SpringDataReportRepository springDataReportRepository;
    private final ReportPersistenceMapper reportPersistenceMapper;

    @Override
    public Report save(Report report) {
        ReportEntity entity = reportPersistenceMapper.toEntity(report);
        ReportEntity saved = springDataReportRepository.save(entity);
        return reportPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Report> findById(UUID id) {
        return springDataReportRepository.findById(id)
                .map(reportPersistenceMapper::toDomain);
    }

    @Override
    public PagedResult<Report> findAll(PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findAll(pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Report> findByUserId(UUID userId, PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findByUserId(userId, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Report> getByStatus(ReportStatus status, PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findByStatus(status, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Report> getByPriority(ReportPriority priority, PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findByPriority(priority, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Report> getByType(ReportType type, PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findByType(type, pageable);
        return toPagedResult(page);
    }

    @Override
    public PagedResult<Report> getByTypePriorityAndStatus(ReportType type, ReportPriority priority, ReportStatus status, PaginationCommand paginationCommand) {
        Pageable pageable = toPageable(paginationCommand);
        Page<ReportEntity> page = springDataReportRepository.findByTypeAndPriorityAndStatus(type, priority, status, pageable);
        return toPagedResult(page);
    }

    private Pageable toPageable(PaginationCommand command) {
        return PageRequest.of(command.page(), command.size());
    }

    private PagedResult<Report> toPagedResult(Page<ReportEntity> page) {
        return new PagedResult<>(
                page.getContent().stream()
                        .map(reportPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
