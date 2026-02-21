package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.report.port.in.dto.CreateReportCommand;
import com.libreuml.backend.application.report.port.in.dto.ResponseAndUpdateStatusCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportPriorityCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportStatusCommand;
import com.libreuml.backend.application.report.port.service.ReportService;
import com.libreuml.backend.domain.model.Report;
import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportStatus;
import com.libreuml.backend.domain.model.ReportType;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.CreateReportRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.ResponseReportRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.UpdateReportPriorityRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.report.UpdateReportStatusRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.report.ReportResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.ReportWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportWebMapper reportWebMapper;

    @PostMapping
    public ResponseEntity<Void> createReport(
            @RequestBody @Valid CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateReportCommand command = reportWebMapper.toCreateReportCommand(request, userDetails.getId());
        reportService.createReport(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/my")
    public ResponseEntity<PagedResult<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationCommand paginationCommand = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<Report> pagedResult = reportService.findByUserId(userDetails.getId(), paginationCommand);
        return ResponseEntity.ok(reportWebMapper.toPagedResponse(pagedResult));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportById(@PathVariable UUID id) {
        Report report = reportService.findById(id);
        return ResponseEntity.ok(reportWebMapper.toResponse(report));
    }

    // ==========================================
    // 🛡️ ADMIN ENDPOINTS
    // ==========================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResult<ReportResponse>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type
    ) {
        PaginationCommand paginationCommand = new PaginationCommand(page, size, sortBy, direction);
        PagedResult<Report> pagedResult;

        if (type != null && priority != null && status != null) {
            pagedResult = reportService.getByTypePriorityAndStatus(
                    ReportType.valueOf(type.toUpperCase()),
                    ReportPriority.valueOf(priority.toUpperCase()),
                    ReportStatus.valueOf(status.toUpperCase()),
                    paginationCommand
            );
        } else if (status != null) {
            pagedResult = reportService.getByStatus(ReportStatus.valueOf(status.toUpperCase()), paginationCommand);
        } else if (priority != null) {
            pagedResult = reportService.getByPriority(ReportPriority.valueOf(priority.toUpperCase()), paginationCommand);
        } else if (type != null) {
            pagedResult = reportService.getByType(ReportType.valueOf(type.toUpperCase()), paginationCommand);
        } else {
            pagedResult = reportService.getByStatus(ReportStatus.OPEN, paginationCommand);
        }

        return ResponseEntity.ok(reportWebMapper.toPagedResponse(pagedResult));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateReportStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        UpdateReportStatusCommand command = reportWebMapper.toUpdateReportStatuscommand(request, id, admin.getId());
        Report updatedReport = reportService.updateReportStatus(command);
        return ResponseEntity.ok(reportWebMapper.toResponse(updatedReport));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> updatePriority(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateReportPriorityRequest request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        UpdateReportPriorityCommand command = reportWebMapper.toUpdateReportPriorityCommand(request, id, admin.getId());
        Report updatedReport = reportService.updateReportPriority(command);
        return ResponseEntity.ok(reportWebMapper.toResponse(updatedReport));
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> respondToReport(
            @PathVariable UUID id,
            @RequestBody @Valid ResponseReportRequest request,
            @AuthenticationPrincipal CustomUserDetails admin
    ) {
        ResponseAndUpdateStatusCommand command = reportWebMapper.toResponseAndUpdateStatusCommand(request, id, admin.getId());
        Report updatedReport = reportService.responseAndUpdateStatus(command);
        return ResponseEntity.ok(reportWebMapper.toResponse(updatedReport));
    }
}