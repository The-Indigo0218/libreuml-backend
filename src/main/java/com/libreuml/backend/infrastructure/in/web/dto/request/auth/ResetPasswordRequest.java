package com.libreuml.backend.infrastructure.in.web.dto.request.auth;

import com.libreuml.backend.infrastructure.in.web.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @ValidPassword String newPassword
) {}
