package com.libreuml.backend.application.resource.port.in.dto;

import com.libreuml.backend.application.common.dto.PaginationCommand;

import java.util.UUID;

public record GetResourceByCreatorIdCommand(UUID creatorId, PaginationCommand pagination) {
}
