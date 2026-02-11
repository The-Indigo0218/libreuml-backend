package com.libreuml.backend.application.resource.port.in.dto;

import com.libreuml.backend.application.common.dto.PaginationCommand;

public record GetResourceByTitleCommand(String title, PaginationCommand pagination) {
}
