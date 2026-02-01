package com.libreuml.backend.application.user.port.in.dto;

import com.libreuml.backend.domain.model.RoleEnum;
import lombok.Builder;

/**
 * DTO carrying the data required to create a new user.
 * This belongs to the Input Port.
 */
@Builder
public record CreateUserCommand(
        String email,
        String password,
        String fullName,
        RoleEnum role
) {}