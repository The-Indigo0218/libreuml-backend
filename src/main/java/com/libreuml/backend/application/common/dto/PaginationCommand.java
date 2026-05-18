package com.libreuml.backend.application.common.dto;

public record PaginationCommand(
        int page,
        int size,
        String sortBy,
        String direction
) {
    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of(
            "createdAt", "updatedAt", "title", "position", "priority", "status", "type"
    );

    public PaginationCommand {
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) sortBy = "createdAt";
        if (direction == null || !direction.matches("(?i)ASC|DESC")) direction = "DESC";
    }
}