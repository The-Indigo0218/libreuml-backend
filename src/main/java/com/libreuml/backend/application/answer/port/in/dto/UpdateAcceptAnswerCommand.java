package com.libreuml.backend.application.answer.port.in.dto;

import java.util.UUID;

public record UpdateAcceptAnswerCommand(UUID userId, UUID answerId) {
}
