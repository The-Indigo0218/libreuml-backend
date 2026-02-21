package com.libreuml.backend.infrastructure.in.web.dto.request.report;

import jakarta.validation.constraints.NotBlank;

public record UpdateReportPriorityRequest(
        @NotBlank
        String priority
) {
}
