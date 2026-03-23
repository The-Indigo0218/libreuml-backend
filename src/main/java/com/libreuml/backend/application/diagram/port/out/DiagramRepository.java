package com.libreuml.backend.application.diagram.port.out;

import com.libreuml.backend.domain.model.Diagram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagramRepository {
    Diagram save(Diagram diagram);
    Optional<Diagram> findById(UUID id);
    List<Diagram> findByOwnerId(UUID ownerId);
    void deleteById(UUID id);
}
