package com.libreuml.backend.application.project.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.ProjectFull;
import com.libreuml.backend.domain.model.Project;

import java.util.UUID;

public interface GetProjectUseCase {
    Project findById(UUID projectId, UUID requesterId);
    PagedResult<Project> listByOwner(UUID ownerId, int page, int size);
    ProjectFull getFull(UUID projectId, UUID requesterId);
}
