package com.libreuml.backend.application.project.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.domain.model.Project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {
    Project save(Project project);
    Optional<Project> findById(UUID id);
    PagedResult<ProjectSummary> findSummariesByOwnerId(UUID ownerId, int page, int size);
    void deleteById(UUID id);
    void touchUpdatedAt(UUID projectId);
}
