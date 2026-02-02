package com.libreuml.backend.application.common.dto;

public record PaginationCommand(
        int page,
        int size
) {
    public PaginationCommand {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100;
    }
}