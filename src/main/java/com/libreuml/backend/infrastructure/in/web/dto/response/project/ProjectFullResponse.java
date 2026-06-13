package com.libreuml.backend.infrastructure.in.web.dto.response.project;

import java.util.List;

/**
 * Whole-workspace payload for {@code GET /projects/{id}/full}: the project, its semantic model and
 * every diagram, matching the shape {@code loadFromCloud} reads on the frontend.
 */
public record ProjectFullResponse(
        ProjectDetailResponse project,
        ModelResponse model,
        List<CloudDiagramResponse> diagrams
) {}
