package com.libreuml.backend.application.project.port.service;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.application.project.dto.UpdateProjectCommand;
import com.libreuml.backend.application.project.exception.ProjectConflictException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.in.CreateProjectUseCase;
import com.libreuml.backend.application.project.port.in.DeleteProjectUseCase;
import com.libreuml.backend.application.project.port.in.GetProjectUseCase;
import com.libreuml.backend.application.project.port.in.UpdateProjectUseCase;
import com.libreuml.backend.application.emailverification.exception.EmailNotVerifiedException;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.projectdiagram.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.projectmodel.port.out.ProjectModelRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectModel;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService implements CreateProjectUseCase, GetProjectUseCase,
        UpdateProjectUseCase, DeleteProjectUseCase {

    private final ProjectRepository projectRepository;
    private final ProjectModelRepository projectModelRepository;
    private final ProjectDiagramRepository projectDiagramRepository;
    private final UserRepository userRepository;

    @Override
    public CreatedProject create(CreateProjectCommand command) {
        User user = userRepository.getUserById(command.ownerId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.ownerId()));
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email verification required to create cloud projects.");
        }

        Project project = Project.create(
                command.ownerId(),
                command.name(),
                command.description(),
                command.author(),
                command.projectVersion(),
                command.targetLanguage(),
                command.basePackage(),
                command.projectKind()
        );
        Project saved = projectRepository.save(project);

        ProjectModel model = ProjectModel.createEmpty(saved.getId());
        ProjectModel savedModel = projectModelRepository.save(model);

        return new CreatedProject(saved, savedModel);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ProjectSummary> listByOwner(UUID ownerId, int page, int size) {
        return projectRepository.findSummariesByOwnerId(ownerId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Project findById(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFull findFull(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);

        ProjectModel model = projectModelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Model not found for project: " + projectId));

        List<ProjectDiagram> diagrams = projectDiagramRepository.findAllByProjectId(projectId);

        return new ProjectFull(project, model, diagrams);
    }

    @Override
    public Project update(UpdateProjectCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));

        project.assertOwner(command.requesterId());

        if (project.getVersion() != command.version()) {
            throw new ProjectConflictException(
                    "Project was modified by another session.",
                    project.getVersion());
        }

        project.updateMetadata(
                command.name(),
                command.description(),
                command.author(),
                command.projectVersion(),
                command.targetLanguage(),
                command.basePackage(),
                command.requesterId()
        );

        return projectRepository.save(project);
    }

    @Override
    public void delete(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);
        projectRepository.deleteById(projectId);
    }
}
