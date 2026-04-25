package com.libreuml.backend.infrastructure.in.web.dto.request.auth;

import com.libreuml.backend.infrastructure.in.web.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "The name is obligatory")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String fullName,

        @NotBlank(message = "the email is obligatory")
        @Email(message = "Invalid email format")
        String email,

        @ValidPassword
        String password,

        @NotBlank(message = "Roles es obligatory (STUDENT o TEACHER)")
        String role

) {}