package com.libreuml.backend.infrastructure.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is obligatory")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is obligatory")
        String password
) {}