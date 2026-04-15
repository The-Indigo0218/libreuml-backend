package com.libreuml.backend.application.apikey.dto;

import java.util.UUID;

public record UpdatePartnerKeyStatusCommand(UUID keyId, boolean active, UUID adminId) {}
