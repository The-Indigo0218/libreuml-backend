package com.libreuml.backend.application.port.in.dto;

import lombok.Builder;

@Builder
public record LoginCommand(String email, String password) {}
