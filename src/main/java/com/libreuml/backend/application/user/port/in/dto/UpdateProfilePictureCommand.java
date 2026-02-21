package com.libreuml.backend.application.user.port.in.dto;

import java.util.UUID;

public record UpdateProfilePictureCommand(UUID id, String avatarUrl) {}
