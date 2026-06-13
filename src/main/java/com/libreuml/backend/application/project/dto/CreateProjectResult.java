package com.libreuml.backend.application.project.dto;

import com.libreuml.backend.domain.model.Project;

import java.util.UUID;

/**
 * Result of creating a project. Bundles the persisted {@link Project} with the id of the empty
 * {@link com.libreuml.backend.domain.model.SemanticModel} created alongside it, so the controller
 * can return {@code {id, modelId, version, createdAt}} — the shape the frontend reads before its
 * first {@code PATCH /projects/{id}/model} call.
 */
public record CreateProjectResult(
        Project project,
        UUID modelId
) {}
