package com.libreuml.backend.application.apikey.dto;

import com.libreuml.backend.domain.model.ApiKey;

import java.util.UUID;

public record CreatePartnerKeyCommand(
        UUID    adminId,
        String  name,
        ApiKey.Scope scope,
        String  partnerName,
        String  partnerEmail,
        int     rateLimitRead,
        int     rateLimitWrite,
        String  redemptionCode,
        Integer redemptionLimit,
        String  notes
) {}
