package com.libreuml.backend.infrastructure.in.web.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(@NotBlank String token) {}
