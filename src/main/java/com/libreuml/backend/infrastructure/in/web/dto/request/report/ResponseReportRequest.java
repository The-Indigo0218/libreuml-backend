package com.libreuml.backend.infrastructure.in.web.dto.request.report;

import jakarta.validation.constraints.NotBlank;

public record ResponseReportRequest(
        @NotBlank
        String adminResponse,
        @NotBlank
        String internalNotes,
        @NotBlank
        String status
        ) {
}
