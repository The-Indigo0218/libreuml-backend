package com.libreuml.backend.infrastructure.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "The name is obligatory")
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String fullName,

        @NotBlank(message = "the email is obligatory")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is obligatory")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
                message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
        String password,

        @NotBlank(message = "Roles es obligatory (STUDENT o TEACHER)")
        String role

) {}