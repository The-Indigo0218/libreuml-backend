package com.libreuml.backend.application.projectdiagram.port.in;

import com.libreuml.backend.domain.model.ProjectDiagram;

import java.util.List;
import java.util.UUID;

public interface GetProjectDiagramUseCase {
    List<ProjectDiagram> listByProject(UUID projectId, UUID requesterId);
    ProjectDiagram findById(UUID projectId, UUID diagramId, UUID requesterId);
}
