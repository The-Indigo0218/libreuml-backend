package com.libreuml.backend.application.projectdiagram.port.service;

import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.projectdiagram.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.application.projectdiagram.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.application.projectdiagram.exception.ProjectDiagramConflictException;
import com.libreuml.backend.application.projectdiagram.exception.ProjectDiagramNotFoundException;
import com.libreuml.backend.application.projectdiagram.port.in.CreateProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.DeleteProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.GetProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.UpdateProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.out.ProjectDiagramRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectDiagramService implements CreateProjectDiagramUseCase, GetProjectDiagramUseCase,
        UpdateProjectDiagramUseCase, DeleteProjectDiagramUseCase {

    private final ProjectDiagramRepository projectDiagramRepository;
    private final ProjectRepository projectRepository;

    @Override
    public ProjectDiagram create(CreateProjectDiagramCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));
        project.assertOwner(command.requesterId());

        ProjectDiagram diagram = ProjectDiagram.create(
                command.projectId(),
                command.name(),
                command.diagramType(),
                command.path(),
                command.viewData()
        );
        return projectDiagramRepository.save(diagram);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDiagram> listByProject(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);
        return projectDiagramRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDiagram findById(UUID projectId, UUID diagramId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);
        return projectDiagramRepository.findByProjectIdAndId(projectId, diagramId)
                .orElseThrow(() -> new ProjectDiagramNotFoundException("Diagram not found: " + diagramId));
    }

    @Override
    public ProjectDiagram update(UpdateProjectDiagramCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));
        project.assertOwner(command.requesterId());

        ProjectDiagram diagram = projectDiagramRepository.findByProjectIdAndId(command.projectId(), command.diagramId())
                .orElseThrow(() -> new ProjectDiagramNotFoundException("Diagram not found: " + command.diagramId()));

        if (diagram.getVersion() != command.version()) {
            throw new ProjectDiagramConflictException(
                    "The diagram was modified by another session.",
                    diagram.getVersion());
        }

        diagram.update(command.name(), command.viewData());
        ProjectDiagram saved = projectDiagramRepository.save(diagram);

        projectRepository.touchUpdatedAt(command.projectId());

        return saved;
    }

    @Override
    public void delete(UUID projectId, UUID diagramId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);

        projectDiagramRepository.findByProjectIdAndId(projectId, diagramId)
                .orElseThrow(() -> new ProjectDiagramNotFoundException("Diagram not found: " + diagramId));

        projectDiagramRepository.deleteById(diagramId);
    }
}
