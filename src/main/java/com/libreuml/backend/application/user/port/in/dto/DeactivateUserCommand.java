package com.libreuml.backend.application.user.port.in.dto;

import java.util.UUID;

public record DeactivateUserCommand(UUID id, String currentPassword) {}
