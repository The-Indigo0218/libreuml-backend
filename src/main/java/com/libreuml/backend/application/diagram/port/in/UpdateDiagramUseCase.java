package com.libreuml.backend.application.diagram.port.in;

import com.libreuml.backend.application.diagram.dto.UpdateDiagramCommand;
import com.libreuml.backend.domain.model.Diagram;

public interface UpdateDiagramUseCase {
    Diagram update(UpdateDiagramCommand command);
}
