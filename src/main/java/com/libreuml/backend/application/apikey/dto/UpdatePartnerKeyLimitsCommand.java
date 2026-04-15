package com.libreuml.backend.application.apikey.dto;

import java.util.UUID;

public record UpdatePartnerKeyLimitsCommand(
        UUID keyId,
        int  rateLimitRead,
        int  rateLimitWrite,
        UUID adminId
) {}
