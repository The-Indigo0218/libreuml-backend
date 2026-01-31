package com.libreuml.backend.application.port.in.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record GetUserByIdCommand(UUID id) {
}
