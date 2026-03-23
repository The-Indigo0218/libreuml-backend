package com.libreuml.backend.application.diagram.port.in;

import java.util.UUID;

public interface DeleteDiagramUseCase {
    void delete(UUID diagramId, UUID requesterId);
}
