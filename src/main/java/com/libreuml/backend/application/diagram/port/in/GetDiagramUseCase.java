package com.libreuml.backend.application.diagram.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.Diagram;

import java.util.UUID;

public interface GetDiagramUseCase {
    Diagram findById(UUID diagramId, UUID requesterId);
    PagedResult<Diagram> listByOwner(UUID ownerId, int page, int size);
}
