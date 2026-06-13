package com.libreuml.backend.infrastructure.in.web.dto.request.auth;

import com.libreuml.backend.infrastructure.in.web.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @StrongPassword String newPassword
) {}
