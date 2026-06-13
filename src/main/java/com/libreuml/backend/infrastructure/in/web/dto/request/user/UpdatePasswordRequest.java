package com.libreuml.backend.infrastructure.in.web.dto.request.user;

import com.libreuml.backend.infrastructure.in.web.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @StrongPassword
        String newPassword
) {}
