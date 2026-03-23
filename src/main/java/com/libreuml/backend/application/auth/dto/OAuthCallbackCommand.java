package com.libreuml.backend.application.auth.dto;

public record OAuthCallbackCommand(
        String code,
        String state,
        String redirectUri,
        OAuthProvider provider,
        String ipAddress,
        String userAgent
) {}
