package com.libreuml.backend.application.project.port.in;

import java.util.UUID;

public interface DeleteProjectDiagramUseCase {
    void delete(UUID projectId, UUID diagramId, UUID requesterId);
}
