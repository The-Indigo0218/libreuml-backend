package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.dto.CreateProjectResult;

public interface CreateProjectUseCase {
    CreateProjectResult create(CreateProjectCommand command);
}
