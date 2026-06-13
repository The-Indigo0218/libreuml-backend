package com.libreuml.backend.application.project.dto;

import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagramType;

import java.util.List;

/**
 * A project plus the aggregate counts the list view needs ({@code diagramCount},
 * {@code diagramTypes}). These are resolved with a single batched query over the whole page rather
 * than per-project, avoiding an N+1.
 */
public record ProjectSummary(
        Project project,
        int diagramCount,
        List<ProjectDiagramType> diagramTypes
) {}
