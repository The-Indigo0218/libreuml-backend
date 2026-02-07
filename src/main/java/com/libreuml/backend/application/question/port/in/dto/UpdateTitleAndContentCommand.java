package com.libreuml.backend.application.question.port.in.dto;

import java.util.UUID;

public record UpdateTitleAndContentCommand(UUID id, String title, String content, UUID creatorId) {}
