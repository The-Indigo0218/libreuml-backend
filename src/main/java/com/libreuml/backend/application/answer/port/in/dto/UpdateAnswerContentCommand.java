package com.libreuml.backend.application.answer.port.in.dto;

import java.util.UUID;

public record UpdateAnswerContentCommand(UUID id, UUID user, String content) {
}
