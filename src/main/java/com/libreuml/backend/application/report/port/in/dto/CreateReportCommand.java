package com.libreuml.backend.application.report.port.in.dto;

import com.libreuml.backend.domain.model.ReportPriority;
import com.libreuml.backend.domain.model.ReportType;

import java.util.Set;
import java.util.UUID;

public record CreateReportCommand(
    ReportType type,
    String title,
    String description,
    UUID userId,
    Set<String> evidencesImages
) {
}
