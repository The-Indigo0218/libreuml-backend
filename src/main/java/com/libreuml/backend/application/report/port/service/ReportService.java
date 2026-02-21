package com.libreuml.backend.application.report.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.exception.UserNotAuthorizedException;
import com.libreuml.backend.application.report.port.in.CreateReportUseCase;
import com.libreuml.backend.application.report.port.in.GetReportUseCase;
import com.libreuml.backend.application.report.port.in.UpdateReportUseCase;
import com.libreuml.backend.application.report.port.in.dto.CreateReportCommand;
import com.libreuml.backend.application.report.port.in.dto.ResponseAndUpdateStatusCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportPriorityCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportStatusCommand;
import com.libreuml.backend.application.report.port.mapper.ReportMapper;
import com.libreuml.backend.application.report.port.out.ReportRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService implements CreateReportUseCase, UpdateReportUseCase, GetReportUseCase {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final UserRepository userRepository;

    @Override
    public Report createReport(CreateReportCommand command) {
        validateUserExists(command.userId());
        Report report = reportMapper.toDomain(command);
        return reportRepository.save(report);
    }

    @Override
    public Report findById(UUID id) {
        return getReportOrThrow(id);
    }

    @Override
    public PagedResult<Report> findByUserId(UUID userId, PaginationCommand paginationCommand) {
        validateUserExists(userId);
        return reportRepository.findByUserId(userId, paginationCommand);
    }

    @Override
    public PagedResult<Report> getByStatus(ReportStatus status, PaginationCommand paginationCommand) {
        return reportRepository.getByStatus(status, paginationCommand);
    }

    @Override
    public PagedResult<Report> getByPriority(ReportPriority priority, PaginationCommand paginationCommand) {
        return reportRepository.getByPriority(priority, paginationCommand);
    }

    @Override
    public PagedResult<Report> getByType(ReportType type, PaginationCommand paginationCommand) {
        return reportRepository.getByType(type, paginationCommand);
    }

    @Override
    public PagedResult<Report> getByTypePriorityAndStatus(ReportType type, ReportPriority priority, ReportStatus status, PaginationCommand paginationCommand) {
        return reportRepository.getByTypePriorityAndStatus(type, priority, status, paginationCommand);
    }

    @Override
    public Report updateReportStatus(UpdateReportStatusCommand command) {
        User admin = validateUserExists(command.responderId());
        verifyAdmin(admin);
        Report report = getReportOrThrow(command.id());
        reportMapper.updateReportStatus(command, report);
        if (report.getStatus() == ReportStatus.RESOLVED) {
            report.solveReportTime();
        }
        return reportRepository.save(report);
    }

    @Override
    public Report responseAndUpdateStatus(ResponseAndUpdateStatusCommand command) {
        User admin = validateUserExists(command.responderId());
        verifyAdmin(admin);
        Report report = getReportOrThrow(command.id());
        reportMapper.respondReport(command, report);
        report.solveReportTime();
        return reportRepository.save(report);
    }

    @Override
    public Report updateReportPriority(UpdateReportPriorityCommand command) {
        User admin = validateUserExists(command.responderId());
        verifyAdmin(admin);
        Report report = getReportOrThrow(command.id());
        reportMapper.updateReportPriority(command, report);
        return reportRepository.save(report);
    }

    private User validateUserExists(UUID userId) {
        return userRepository.getUserById(userId).orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
    }


    private Report getReportOrThrow(UUID reportId) {
        return reportRepository.findById(reportId).orElseThrow(() -> new RuntimeException("Report with id " + reportId + " not found"));
    }

    private void verifyAdmin(User user) {
        if (!user.getRole().equals(RoleEnum.ADMIN)) {
            throw new UserNotAuthorizedException("User with id " + user.getId() + " is not authorized to perform this action");
        }
    }
}
