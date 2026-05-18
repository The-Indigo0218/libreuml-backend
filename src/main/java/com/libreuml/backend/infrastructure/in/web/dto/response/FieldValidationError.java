package com.libreuml.backend.infrastructure.in.web.dto.response;

public record FieldValidationError(
        String field,
        Object rejectedValue,
        String message
) {}
