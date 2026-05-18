package com.libreuml.backend.infrastructure.in.web.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateReportRequest(
        @NotBlank
        @Size(max = 50)
        String type,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 5000)
        String description,

        Set<String> evidencesImages
) {
}
