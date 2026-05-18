package com.libreuml.backend.infrastructure.in.web.dto.request.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemPartnerCodeRequest(@NotBlank @Size(max = 50) String redemptionCode) {}
