package com.libreuml.backend.application.auth.dto;

public record TokenPair(String accessToken, String rawRefreshToken) {}
