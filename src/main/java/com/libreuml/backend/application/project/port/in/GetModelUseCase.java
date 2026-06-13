package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.domain.model.SemanticModel;

import java.util.UUID;

public interface GetModelUseCase {
    SemanticModel findByProjectId(UUID projectId, UUID requesterId);
}
