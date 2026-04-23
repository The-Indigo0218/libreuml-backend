package com.libreuml.backend.application.projectmodel.port.in;

import com.libreuml.backend.domain.model.ProjectModel;

import java.util.UUID;

public interface GetProjectModelUseCase {
    ProjectModel findByProjectId(UUID projectId, UUID requesterId);
}
