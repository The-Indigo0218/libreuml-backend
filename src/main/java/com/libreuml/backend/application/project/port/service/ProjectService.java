package com.libreuml.backend.application.project.port.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.emailverification.exception.EmailNotVerifiedException;
import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.dto.CreateProjectResult;
import com.libreuml.backend.application.project.dto.ProjectFull;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.application.project.dto.UpdateProjectCommand;
import com.libreuml.backend.application.project.exception.ModelNotFoundException;
import com.libreuml.backend.application.project.exception.ProjectConflictException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.in.CreateProjectUseCase;
import com.libreuml.backend.application.project.port.in.DeleteProjectUseCase;
import com.libreuml.backend.application.project.port.in.GetProjectUseCase;
import com.libreuml.backend.application.project.port.in.UpdateProjectUseCase;
import com.libreuml.backend.application.project.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.out.SemanticModelRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectDiagramType;
import com.libreuml.backend.domain.model.SemanticModel;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for the {@link Project} aggregate and its full-graph read.
 *
 * <p>Quota accounting mirrors {@code DiagramService}: storage is charged on the bytes of the
 * project's {@code vfsSnapshot}. The semantic model's data and each diagram's view data are
 * charged by {@code ModelService} and {@code ProjectDiagramService} respectively, so each write
 * path owns its own slice of the user's quota.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService implements CreateProjectUseCase, GetProjectUseCase,
        UpdateProjectUseCase, DeleteProjectUseCase {

    private final ProjectRepository projectRepository;
    private final SemanticModelRepository modelRepository;
    private final ProjectDiagramRepository projectDiagramRepository;
    private final UserRepository userRepository;
    private final MetricsPort metricsPort;
    private final ObjectMapper objectMapper;

    @Override
    public CreateProjectResult create(CreateProjectCommand command) {
        User user = userRepository.getUserById(command.ownerId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.ownerId()));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email must be verified before saving projects.");
        }

        // Domain invariant first: Project.create() enforces the 5 MB vfsSnapshot ceiling before
        // the quota check, so oversized snapshots report the domain error rather than a quota error.
        Project project = Project.create(
                command.ownerId(), command.name(), command.description(), command.author(),
                command.projectVersion(), command.projectKind(),
                command.targetLanguage(), command.basePackage(), command.vfsSnapshot());

        long payloadBytes = jsonBytes(command.vfsSnapshot());
        if (!user.hasQuotaFor(payloadBytes)) {
            throw new QuotaExceededException(
                    "Storage quota exceeded. Quota: " + user.getStorageQuotaBytes()
                    + " bytes, already used: " + user.getStorageUsedBytes()
                    + " bytes, requested: " + payloadBytes + " bytes.");
        }

        Project savedProject = projectRepository.save(project);

        // Persist the empty semantic model alongside the project (1:1). The frontend fills it via
        // PATCH /projects/{id}/model right after this call returns.
        SemanticModel model = SemanticModel.createEmpty(savedProject.getId(), objectMapper.createObjectNode());
        SemanticModel savedModel = modelRepository.save(model);

        user.incrementUsage(payloadBytes);
        userRepository.save(user);

        metricsPort.incrementProjectSaved(savedProject.getProjectKind());
        metricsPort.observeUserStorageBytes(user.getStorageUsedBytes());
        return new CreateProjectResult(savedProject, savedModel.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Project findById(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        if (!project.isAccessibleBy(requesterId)) {
            throw new ProjectOwnershipException("Access denied to project: " + projectId);
        }
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ProjectSummary> listByOwner(UUID ownerId, int page, int size) {
        PagedResult<Project> projects = projectRepository.findAllByOwnerId(ownerId, page, size);

        List<UUID> projectIds = projects.content().stream().map(Project::getId).toList();
        Map<UUID, List<ProjectDiagramType>> typesByProject =
                projectDiagramRepository.findDiagramTypesByProjectIds(projectIds);

        List<ProjectSummary> summaries = projects.content().stream().map(project -> {
            List<ProjectDiagramType> types = typesByProject.getOrDefault(project.getId(), List.of());
            List<ProjectDiagramType> distinct = types.stream().distinct().toList();
            return new ProjectSummary(project, types.size(), distinct);
        }).toList();

        return new PagedResult<>(summaries, projects.pageNumber(), projects.pageSize(),
                projects.totalElements(), projects.totalPages(), projects.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFull getFull(UUID projectId, UUID requesterId) {
        Project project = findById(projectId, requesterId);
        SemanticModel model = modelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ModelNotFoundException("Model not found for project: " + projectId));
        List<ProjectDiagram> diagrams = projectDiagramRepository.findAllByProjectId(projectId);
        return new ProjectFull(project, model, diagrams);
    }

    @Override
    public Project update(UpdateProjectCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));

        /*
         * Application-level optimistic concurrency guard, identical in spirit to DiagramService:
         * fail fast with a deterministic 409 when the client's version is stale, while JPA's
         * @Version remains the final safety net for genuine concurrent flushes.
         */
        if (project.getVersion() != command.version()) {
            throw new ProjectConflictException(
                    "Version mismatch: client sent " + command.version()
                    + " but current version is " + project.getVersion()
                    + ". Reload the project and retry.");
        }

        if (command.vfsSnapshot() != null) {
            long delta = jsonBytes(command.vfsSnapshot()) - jsonBytes(project.getVfsSnapshot());
            applyQuotaDelta(command.requesterId(), delta);
        }

        project.update(command.name(), command.description(), command.author(),
                command.targetLanguage(), command.basePackage(),
                command.vfsSnapshot(), command.requesterId());
        return projectRepository.save(project);
    }

    @Override
    public void delete(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.delete(requesterId);

        // Reclaim every byte the project charged against the user's quota: snapshot + model + diagrams.
        long reclaimed = jsonBytes(project.getVfsSnapshot());
        reclaimed += modelRepository.findByProjectId(projectId)
                .map(m -> jsonBytes(m.getData())).orElse(0L);
        for (ProjectDiagram diagram : projectDiagramRepository.findAllByProjectId(projectId)) {
            reclaimed += jsonBytes(diagram.getViewData());
        }

        // The model and diagrams rows are removed by the ON DELETE CASCADE FKs on project_id.
        projectRepository.deleteById(projectId);

        User user = userRepository.getUserById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + requesterId));
        user.decrementUsage(reclaimed);
        userRepository.save(user);
        metricsPort.observeUserStorageBytes(user.getStorageUsedBytes());
    }

    /** Charges (delta > 0) or refunds (delta < 0) the requester's quota; throws 422 if over budget. */
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
