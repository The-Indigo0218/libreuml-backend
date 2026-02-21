package com.libreuml.backend.application.user.port.in.dto;

import java.util.UUID;

public record UpdateEmailCommand(UUID id, String email, String currentPassword) {}
