package com.libreuml.backend.infrastructure.in.web.dto.request.apikey;

import jakarta.validation.constraints.NotNull;

public record UpdatePartnerKeyStatusRequest(@NotNull Boolean active) {}
