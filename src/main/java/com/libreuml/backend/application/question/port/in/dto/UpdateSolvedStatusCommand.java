package com.libreuml.backend.application.question.port.in.dto;

import java.util.UUID;

public record UpdateSolvedStatusCommand(UUID id, boolean isSolved, UUID creatorId) {
}
