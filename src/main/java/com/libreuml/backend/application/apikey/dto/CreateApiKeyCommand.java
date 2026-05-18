package com.libreuml.backend.application.apikey.dto;

import com.libreuml.backend.domain.model.ApiKey;

import java.util.UUID;

public record CreateApiKeyCommand(UUID userId, String name, ApiKey.Scope scope) {}
