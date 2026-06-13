package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.UpdateProjectCommand;
import com.libreuml.backend.domain.model.Project;

public interface UpdateProjectUseCase {
    Project update(UpdateProjectCommand command);
}
