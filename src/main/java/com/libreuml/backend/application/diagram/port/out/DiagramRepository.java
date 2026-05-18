package com.libreuml.backend.application.diagram.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Diagram;

import java.util.Optional;
import java.util.UUID;

public interface DiagramRepository {
    Diagram save(Diagram diagram);
    Optional<Diagram> findById(UUID id);
    PagedResult<Diagram> findAllByOwnerId(UUID ownerId, int page, int size);
    void deleteById(UUID id);
    PagedResult<Diagram> findPublicDiagrams(PaginationCommand pagination);
}
