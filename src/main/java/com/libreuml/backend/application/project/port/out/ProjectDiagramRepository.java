package com.libreuml.backend.application.project.port.out;

import com.libreuml.backend.domain.model.ProjectDiagram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDiagramRepository {
    ProjectDiagram save(ProjectDiagram diagram);
    Optional<ProjectDiagram> findById(UUID id);
    List<ProjectDiagram> findAllByProjectId(UUID projectId);
    void deleteById(UUID id);
}
