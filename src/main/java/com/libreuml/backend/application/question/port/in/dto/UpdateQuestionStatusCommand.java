package com.libreuml.backend.application.question.port.in.dto;

import java.util.UUID;

public record UpdateQuestionStatusCommand(UUID id, boolean isActive) {
}
