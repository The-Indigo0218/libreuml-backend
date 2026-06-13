package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.domain.model.ProjectDiagram;

import java.util.List;
import java.util.UUID;

public interface GetProjectDiagramUseCase {
    ProjectDiagram findById(UUID projectId, UUID diagramId, UUID requesterId);
    List<ProjectDiagram> listByProject(UUID projectId, UUID requesterId);
}
