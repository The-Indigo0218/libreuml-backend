package com.libreuml.backend.application.common;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}