package com.libreuml.backend.application.project.port.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.project.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.application.project.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.application.project.exception.ProjectDiagramConflictException;
import com.libreuml.backend.application.project.exception.ProjectDiagramNotFoundException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.in.CreateProjectDiagramUseCase;
import com.libreuml.backend.application.project.port.in.DeleteProjectDiagramUseCase;
import com.libreuml.backend.application.project.port.in.GetProjectDiagramUseCase;
import com.libreuml.backend.application.project.port.in.UpdateProjectDiagramUseCase;
import com.libreuml.backend.application.project.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Application service for {@link ProjectDiagram} aggregates scoped to a project.
 *
 * <p>Diagrams have no owner of their own: access is authorised against the parent {@link Project}
 * (public projects allow reads, only the owner may write). Storage is charged on each diagram's
 * {@code viewData}. Every lookup verifies the diagram actually belongs to the project named in the
 * path, so a valid id under the wrong project resolves to 404 rather than leaking across projects.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectDiagramService implements CreateProjectDiagramUseCase, GetProjectDiagramUseCase,
        UpdateProjectDiagramUseCase, DeleteProjectDiagramUseCase {

    private final ProjectDiagramRepository projectDiagramRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MetricsPort metricsPort;

    @Override
    public ProjectDiagram create(CreateProjectDiagramCommand command) {
        Project project = loadProject(command.projectId());
        assertOwner(project, command.requesterId());

        // Domain invariant first: ProjectDiagram.create() enforces the 5 MB viewData ceiling.
        ProjectDiagram diagram = ProjectDiagram.create(
                command.projectId(), command.name(), command.diagramType(),
                command.path(), command.viewData());

        long payloadBytes = jsonBytes(command.viewData());
        chargeQuota(command.requesterId(), payloadBytes);

        ProjectDiagram saved = projectDiagramRepository.save(diagram);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDiagram findById(UUID projectId, UUID diagramId, UUID requesterId) {
        Project project = loadProject(projectId);
        if (!project.isAccessibleBy(requesterId)) {
            throw new ProjectOwnershipException("Access denied to project: " + projectId);
        }
        return loadDiagramOfProject(diagramId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDiagram> listByProject(UUID projectId, UUID requesterId) {
        Project project = loadProject(projectId);
        if (!project.isAccessibleBy(requesterId)) {
            throw new ProjectOwnershipException("Access denied to project: " + projectId);
        }
        return projectDiagramRepository.findAllByProjectId(projectId);
    }

    @Override
    public ProjectDiagram update(UpdateProjectDiagramCommand command) {
        Project project = loadProject(command.projectId());
        assertOwner(project, command.requesterId());

        ProjectDiagram diagram = loadDiagramOfProject(command.diagramId(), command.projectId());

        if (diagram.getVersion() != command.version()) {
            throw new ProjectDiagramConflictException(
                    "Version mismatch: client sent " + command.version()
                    + " but current version is " + diagram.getVersion()
                    + ". Reload the diagram and retry.");
        }

        if (command.viewData() != null) {
            long delta = jsonBytes(command.viewData()) - jsonBytes(diagram.getViewData());
            applyQuotaDelta(command.requesterId(), delta);
        }

        diagram.update(command.name(), command.viewData());
        return projectDiagramRepository.save(diagram);
    }

    @Override
    public void delete(UUID projectId, UUID diagramId, UUID requesterId) {
        Project project = loadProject(projectId);
        assertOwner(project, requesterId);

        ProjectDiagram diagram = loadDiagramOfProject(diagramId, projectId);
        long reclaimed = jsonBytes(diagram.getViewData());

        projectDiagramRepository.deleteById(diagramId);

        User user = userRepository.getUserById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterId));
        user.decrementUsage(reclaimed);
        userRepository.save(user);
        metricsPort.observeUserStorageBytes(user.getStorageUsedBytes());
    }

    private Project loadProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
    }

    /** Loads a diagram and verifies it belongs to the given project, else 404. */
    private ProjectDiagram loadDiagramOfProject(UUID diagramId, UUID projectId) {
        ProjectDiagram diagram = projectDiagramRepository.findById(diagramId)
                .orElseThrow(() -> new ProjectDiagramNotFoundException("Diagram not found: " + diagramId));
        if (!diagram.getProjectId().equals(projectId)) {
            throw new ProjectDiagramNotFoundException(
                    "Diagram " + diagramId + " does not belong to project " + projectId + ".");
        }
        return diagram;
    }

    private void assertOwner(Project project, UUID requesterId) {
        if (!project.getOwnerId().equals(requesterId)) {
            throw new ProjectOwnershipException("Only the project owner can modify its diagrams.");
        }
    }

    private void chargeQuota(UUID requesterId, long bytes) {
        if (bytes <= 0) return;
        User owner = userRepository.getUserById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterId));
        if (!owner.hasQuotaFor(bytes)) {
            throw new QuotaExceededException(
                    "Storage quota exceeded. Quota: " + owner.getStorageQuotaBytes()
                    + " bytes, already used: " + owner.getStorageUsedBytes()
                    + " bytes, requested: " + bytes + " bytes.");
        }
        owner.incrementUsage(bytes);
        userRepository.save(owner);
        metricsPort.observeUserStorageBytes(owner.getStorageUsedBytes());
    }

    private void applyQuotaDelta(UUID requesterId, long delta) {
        if (delta == 0) return;
        User owner = userRepository.getUserById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterId));
        if (delta > 0 && !owner.hasQuotaFor(delta)) {
            throw new QuotaExceededException(
                    "Storage quota exceeded. Would need " + delta + " more bytes but only "
                    + (owner.getStorageQuotaBytes() - owner.getStorageUsedBytes()) + " bytes remain.");
        }
        if (delta > 0) owner.incrementUsage(delta);
        else owner.decrementUsage(-delta);
        userRepository.save(owner);
        metricsPort.observeUserStorageBytes(owner.getStorageUsedBytes());
    }

    private static long jsonBytes(ObjectNode node) {
        return node != null ? node.toString().getBytes(StandardCharsets.UTF_8).length : 0L;
    }
}
