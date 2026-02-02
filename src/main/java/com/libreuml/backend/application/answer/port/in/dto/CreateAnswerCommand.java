package com.libreuml.backend.application.answer.port.in.dto;

import java.util.List;
import java.util.UUID;

public record CreateAnswerCommand(UUID creatorId, UUID questionId, String content, List<String> imageUrls) {
}
