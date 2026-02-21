package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.report.port.in.dto.CreateReportCommand;
import com.libreuml.backend.application.report.port.in.dto.ResponseAndUpdateStatusCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportPriorityCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportStatusCommand;
import com.libreuml.backend.domain.model.*;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.CreateReportRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.ResponseReportRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.UpdateReportPriorityRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.UpdateReportStatusRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.report.ReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReportWebMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "type", source = "request.type", qualifiedByName = "ValidateReportType")
    CreateReportCommand toCreateReportCommand(CreateReportRequest request, UUID userId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "responderId", source = "responderId")
    @Mapping(target = "status", source = "request.status", qualifiedByName = "ValidateReportStatus")
    ResponseAndUpdateStatusCommand toResponseAndUpdateStatusCommand(ResponseReportRequest request, UUID id, UUID responderId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "responderId", source = "responderId")
    @Mapping(target = "status", source = "request.status", qualifiedByName = "ValidateReportStatus")
    UpdateReportStatusCommand toUpdateReportStatuscommand(UpdateReportStatusRequest request, UUID id, UUID responderId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "responderId", source = "responderId")
    @Mapping(target = "priority", source = "request.priority", qualifiedByName = "ValidateReportPriority")
    UpdateReportPriorityCommand toUpdateReportPriorityCommand(UpdateReportPriorityRequest request, UUID id, UUID responderId);

    @Named("ValidateReportStatus")
    default ReportStatus validateReportStatus(String reportStatus) {
        try {
            return ReportStatus.valueOf(reportStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Report Status. Allowed: OPEN, IN_PROGRESS, RESOLVED, REJECTED");
        }
    }

    @Named("ValidateReportPriority")
    default ReportPriority validateReportPriority(String reportPriority) {
        try {
            return ReportPriority.valueOf(reportPriority.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Report Priority. Allowed: LOW, MEDIUM, HIGH");
        }
    }

    @Named("ValidateReportType")
    default ReportType validateReportType(String reportType) {
        try {
            return ReportType.valueOf(reportType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Report Type. Allowed: BUG, FEATURE_REQUEST, OTHER");
        }
    }

    ReportResponse toResponse(Report report);

    default PagedResult<ReportResponse> toPagedResponse(PagedResult<Report> result) {
        if (result == null) return null;

        List<ReportResponse> mappedContent = result.content().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResult<>(
                mappedContent,
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
    }


}
