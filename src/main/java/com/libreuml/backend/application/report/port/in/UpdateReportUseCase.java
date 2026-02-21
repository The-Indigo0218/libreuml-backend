package com.libreuml.backend.application.report.port.in;

import com.libreuml.backend.application.report.port.in.dto.ResponseAndUpdateStatusCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportPriorityCommand;
import com.libreuml.backend.application.report.port.in.dto.UpdateReportStatusCommand;
import com.libreuml.backend.domain.model.Report;

public interface UpdateReportUseCase {
    Report updateReportStatus(UpdateReportStatusCommand command);
    Report responseAndUpdateStatus(ResponseAndUpdateStatusCommand command);
    Report updateReportPriority(UpdateReportPriorityCommand command);
}
