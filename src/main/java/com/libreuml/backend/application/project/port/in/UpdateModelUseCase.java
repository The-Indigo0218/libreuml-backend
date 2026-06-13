package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.UpdateModelCommand;
import com.libreuml.backend.domain.model.SemanticModel;

public interface UpdateModelUseCase {
    SemanticModel update(UpdateModelCommand command);
}
