package com.libreuml.backend.application.projectdiagram.port.in;

import com.libreuml.backend.application.projectdiagram.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.domain.model.ProjectDiagram;

public interface UpdateProjectDiagramUseCase {
    ProjectDiagram update(UpdateProjectDiagramCommand command);
}
