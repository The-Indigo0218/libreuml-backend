package com.libreuml.backend.infrastructure.in.web.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank(message = "New email is required")
        @Email(message = "Invalid email format")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {}