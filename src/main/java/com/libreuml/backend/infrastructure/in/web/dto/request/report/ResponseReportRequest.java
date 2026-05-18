package com.libreuml.backend.infrastructure.in.web.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResponseReportRequest(
        @NotBlank
        @Size(max = 5000)
        String adminResponse,

        @NotBlank
        @Size(max = 5000)
        String internalNotes,

        @NotBlank
        @Size(max = 50)
        String status
) {
}
