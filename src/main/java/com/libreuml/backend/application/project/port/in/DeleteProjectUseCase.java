package com.libreuml.backend.application.project.port.in;

import java.util.UUID;

public interface DeleteProjectUseCase {
    void delete(UUID projectId, UUID requesterId);
}
