package com.libreuml.backend.application.question.port.in.dto;

import java.util.UUID;

public record UpdateActiveStatusCommand(UUID id, UUID user, String reason) {
}
