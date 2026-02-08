package com.libreuml.backend.application.common.dto;

public record PaginationCommand(
        int page,
        int size,
        String sortBy,
        String direction
) {
    public PaginationCommand {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (direction == null || !direction.matches("(?i)ASC|DESC")) direction = "DESC";
    }
}