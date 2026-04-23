package com.libreuml.backend.application.projectmodel.port.out;

import com.libreuml.backend.domain.model.ProjectModel;

import java.util.Optional;
import java.util.UUID;

public interface ProjectModelRepository {
    ProjectModel save(ProjectModel model);
    Optional<ProjectModel> findByProjectId(UUID projectId);
    long getModelDataBytesByProjectId(UUID projectId);
    long getTotalModelDataBytesByOwner(UUID userId);
}
