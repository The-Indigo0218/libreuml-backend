package com.libreuml.backend.application.projectdiagram.port.in;

import java.util.UUID;

public interface DeleteProjectDiagramUseCase {
    void delete(UUID projectId, UUID diagramId, UUID requesterId);
}
