package com.libreuml.backend.application.project.port.out;

import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectDiagramType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDiagramRepository {
    ProjectDiagram save(ProjectDiagram diagram);
    Optional<ProjectDiagram> findById(UUID id);
    List<ProjectDiagram> findAllByProjectId(UUID projectId);
    void deleteById(UUID id);

    /**
     * Batched lookup of every diagram type per project for the given ids, used to build the list
     * view's {@code diagramCount}/{@code diagramTypes} without an N+1. Returns the types (with
     * repetition) keyed by project id; projects with no diagrams are absent from the map.
     */
    Map<UUID, List<ProjectDiagramType>> findDiagramTypesByProjectIds(Collection<UUID> projectIds);
}
