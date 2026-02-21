package com.libreuml.backend.infrastructure.in.web.dto.response.user;

import com.libreuml.backend.domain.model.RoleEnum;

import java.util.List;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, List<String> academicDegrees, List<String> organization, List<String> stacks, String email, RoleEnum role) {}