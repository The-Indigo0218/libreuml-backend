package com.libreuml.backend.application.question.port.in.dto;

import java.util.List;

public record CreateQuestionCommand(String title, String content, Long authorId, List<String> tags) {
}
