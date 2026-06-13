package com.libreuml.backend.application.project.port.out;

import com.libreuml.backend.domain.model.SemanticModel;

import java.util.Optional;
import java.util.UUID;

public interface SemanticModelRepository {
    SemanticModel save(SemanticModel model);
    Optional<SemanticModel> findByProjectId(UUID projectId);
}
