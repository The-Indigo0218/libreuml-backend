package com.libreuml.backend.application.auth.dto;

public record RefreshCommand(String rawRefreshToken, String ipAddress, String userAgent) {}
