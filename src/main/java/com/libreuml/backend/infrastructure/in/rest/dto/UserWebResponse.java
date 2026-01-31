package com.libreuml.backend.infrastructure.in.rest.dto;

import com.libreuml.backend.domain.model.RoleEnum;
import java.util.UUID;

public record UserWebResponse(
        UUID id,
        String email,
        String fullName,
        RoleEnum role,
        boolean active
) {}