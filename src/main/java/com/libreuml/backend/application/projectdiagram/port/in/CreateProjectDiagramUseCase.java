package com.libreuml.backend.application.projectdiagram.port.in;

import com.libreuml.backend.application.projectdiagram.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.domain.model.ProjectDiagram;

public interface CreateProjectDiagramUseCase {
    ProjectDiagram create(CreateProjectDiagramCommand command);
}
