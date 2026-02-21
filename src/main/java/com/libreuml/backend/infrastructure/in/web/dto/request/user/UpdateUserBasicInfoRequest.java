package com.libreuml.backend.infrastructure.in.web.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUserBasicInfoRequest(
        @NotBlank(message = "Full name cannot be blank")
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        String fullName,

        @Size(max = 20, message = "You can list up to 20 academic degrees")
        List<String> academicDegrees,

        @Size(max = 20, message = "You can list up to 20 organizations")
        List<String> organization,

        @Size(max = 50, message = "You can list up to 50 tech stack items")
        List<String> stacks
) {}