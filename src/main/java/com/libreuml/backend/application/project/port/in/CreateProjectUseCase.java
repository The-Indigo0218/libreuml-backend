package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectModel;

public interface CreateProjectUseCase {
    record CreatedProject(Project project, ProjectModel model) {}
    CreatedProject create(CreateProjectCommand command);
}
