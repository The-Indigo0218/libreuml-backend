package com.libreuml.backend.application.diagram.port.in;

import com.libreuml.backend.application.diagram.dto.CreateDiagramCommand;
import com.libreuml.backend.domain.model.Diagram;

public interface CreateDiagramUseCase {
    Diagram create(CreateDiagramCommand command);
}
