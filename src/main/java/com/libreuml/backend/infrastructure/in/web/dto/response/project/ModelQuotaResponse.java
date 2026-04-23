package com.libreuml.backend.infrastructure.in.web.dto.response.project;

public record ModelQuotaResponse(String error, String message, long used, long quota) {}
