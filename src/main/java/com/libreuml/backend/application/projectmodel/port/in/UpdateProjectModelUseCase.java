package com.libreuml.backend.application.projectmodel.port.in;

import com.libreuml.backend.application.projectmodel.dto.UpdateProjectModelCommand;
import com.libreuml.backend.domain.model.ProjectModel;

public interface UpdateProjectModelUseCase {
    ProjectModel update(UpdateProjectModelCommand command);
}
