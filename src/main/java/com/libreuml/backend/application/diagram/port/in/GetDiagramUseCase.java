package com.libreuml.backend.application.diagram.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Diagram;

import java.util.List;
import java.util.UUID;

public interface GetDiagramUseCase {
    Diagram findById(UUID diagramId, UUID requesterId);
    List<Diagram> listByOwner(UUID ownerId);
    PagedResult<Diagram> listPublic(PaginationCommand pagination);
}
