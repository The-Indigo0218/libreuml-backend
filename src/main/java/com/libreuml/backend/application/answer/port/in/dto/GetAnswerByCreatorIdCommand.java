package com.libreuml.backend.application.answer.port.in.dto;

import com.libreuml.backend.application.common.dto.PaginationCommand;

import java.util.UUID;

public record GetAnswerByCreatorIdCommand(UUID creatorId, PaginationCommand paginationCommand) {
}
