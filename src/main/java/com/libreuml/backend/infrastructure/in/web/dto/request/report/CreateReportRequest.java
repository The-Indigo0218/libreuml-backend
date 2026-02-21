package com.libreuml.backend.infrastructure.in.web.dto.request.report;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateReportRequest(
        @NotBlank
        String type,
        @NotBlank
        String title,
        @NotBlank
        String description,
        Set<String> evidencesImages) {
}
