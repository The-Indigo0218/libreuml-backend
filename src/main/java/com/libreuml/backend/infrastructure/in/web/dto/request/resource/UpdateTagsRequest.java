package com.libreuml.backend.infrastructure.in.web.dto.request.resource;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateTagsRequest(
        @NotNull(message = "Tags list cannot be null")
        List<String> tags
) {}