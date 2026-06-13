package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.domain.model.ProjectDiagram;

public interface CreateProjectDiagramUseCase {
    ProjectDiagram create(CreateProjectDiagramCommand command);
}
