package com.libreuml.backend.infrastructure.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Unified error envelope returned by every error path in the API.
 * The {@code errors} field is omitted from the JSON payload when null (non-validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        String path,
        List<FieldValidationError> errors
) {}
