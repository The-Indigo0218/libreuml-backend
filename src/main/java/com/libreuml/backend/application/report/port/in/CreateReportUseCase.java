package com.libreuml.backend.application.report.port.in;

import com.libreuml.backend.application.report.port.in.dto.CreateReportCommand;
import com.libreuml.backend.domain.model.Report;

public interface CreateReportUseCase {
    Report createReport(CreateReportCommand command);
}
