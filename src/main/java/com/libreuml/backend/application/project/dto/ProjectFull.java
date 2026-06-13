package com.libreuml.backend.application.project.dto;

import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.SemanticModel;

import java.util.List;

/**
 * Aggregated view returned by {@code GET /projects/{id}/full}: a project together with its
 * semantic model and all its diagrams, so the frontend can rebuild the whole workspace in one
 * round trip (see {@code loadFromCloud}).
 */
public record ProjectFull(
        Project project,
        SemanticModel model,
        List<ProjectDiagram> diagrams
) {}
