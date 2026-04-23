package com.libreuml.backend.application.projectdiagram.port.out;

import com.libreuml.backend.domain.model.ProjectDiagram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDiagramRepository {
    ProjectDiagram save(ProjectDiagram diagram);
    Optional<ProjectDiagram> findByProjectIdAndId(UUID projectId, UUID diagramId);
    List<ProjectDiagram> findAllByProjectId(UUID projectId);
    void deleteById(UUID diagramId);
    long getTotalViewDataBytesByOwner(UUID ownerId);
}
