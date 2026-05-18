package com.libreuml.backend.infrastructure.in.web.dto.request.apikey;

import com.libreuml.backend.domain.model.ApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ApiKey.Scope scope
) {}
