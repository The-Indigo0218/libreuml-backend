package com.libreuml.backend.application.answer.port.in.dto;

import java.util.UUID;

public record UpdateActiveAnswerStatusCommand(UUID id, UUID user, String reason) {
}
