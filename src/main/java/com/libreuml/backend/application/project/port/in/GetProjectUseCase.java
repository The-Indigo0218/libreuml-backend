package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectModel;

import java.util.List;
import java.util.UUID;

public interface GetProjectUseCase {
    record ProjectFull(Project project, ProjectModel model, List<ProjectDiagram> diagrams) {}
    PagedResult<ProjectSummary> listByOwner(UUID ownerId, int page, int size);
    Project findById(UUID projectId, UUID requesterId);
    ProjectFull findFull(UUID projectId, UUID requesterId);
}
