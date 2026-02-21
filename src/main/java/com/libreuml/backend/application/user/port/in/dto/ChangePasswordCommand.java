package com.libreuml.backend.application.user.port.in.dto;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID id,
        String currentPassword,
        String newPassword
) {}