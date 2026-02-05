package com.libreuml.backend.application.question.port.in.dto;

import java.util.List;
import java.util.UUID;

public record CreateQuestionCommand(String title, String content, UUID creatorId, List<String> tags, List<String> imageUrls) {
}
