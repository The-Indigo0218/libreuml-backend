package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.domain.model.ProjectDiagram;

public interface UpdateProjectDiagramUseCase {
    ProjectDiagram update(UpdateProjectDiagramCommand command);
}
