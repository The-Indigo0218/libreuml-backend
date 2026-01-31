package com.libreuml.backend.infrastructure.in.rest.dto;

import com.libreuml.backend.domain.model.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @Email(message = "Invalid email format")
        @NotBlank
        String email,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        @NotNull
        RoleEnum role
        ) {}