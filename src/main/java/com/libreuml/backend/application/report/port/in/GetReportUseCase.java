package com.libreuml.backend.application.report.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Report;
import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportStatus;
import com.libreuml.backend.domain.model.ReportType;

import java.util.UUID;

public interface GetReportUseCase {
    Report findById(UUID id);

    PagedResult<Report> findByUserId(UUID userId, PaginationCommand paginationCommand);

    PagedResult<Report> getByStatus(ReportStatus status, PaginationCommand paginationCommand);

    PagedResult<Report> getByPriority(ReportPriority priority, PaginationCommand paginationCommand);

    PagedResult<Report> getByType(ReportType type, PaginationCommand paginationCommand);

    PagedResult<Report> getByTypePriorityAndStatus(ReportType type, ReportPriority priority, ReportStatus status, PaginationCommand paginationCommand);
}
