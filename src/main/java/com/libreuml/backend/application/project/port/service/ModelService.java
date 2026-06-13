package com.libreuml.backend.application.project.port.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.project.dto.UpdateModelCommand;
import com.libreuml.backend.application.project.exception.ModelConflictException;
import com.libreuml.backend.application.project.exception.ModelNotFoundException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.in.GetModelUseCase;
import com.libreuml.backend.application.project.port.in.UpdateModelUseCase;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.out.SemanticModelRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.SemanticModel;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Application service for the {@link SemanticModel} aggregate (one-to-one with a project).
 *
 * <p>The model has no owner of its own: every access is authorised against its parent
 * {@link Project} — public projects allow reads, only the owner may write. Storage is charged on
 * the delta between the old and new model {@code data}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ModelService implements GetModelUseCase, UpdateModelUseCase {

    private final SemanticModelRepository modelRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MetricsPort metricsPort;

    @Override
    @Transactional(readOnly = true)
    public SemanticModel findByProjectId(UUID projectId, UUID requesterId) {
        Project project = loadProject(projectId);
        if (!project.isAccessibleBy(requesterId)) {
            throw new ProjectOwnershipException("Access denied to project: " + projectId);
        }
        return modelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ModelNotFoundException("Model not found for project: " + projectId));
    }

    @Override
    public SemanticModel update(UpdateModelCommand command) {
        Project project = loadProject(command.projectId());
        assertOwner(project, command.requesterId());

        SemanticModel model = modelRepository.findByProjectId(command.projectId())
                .orElseThrow(() -> new ModelNotFoundException(
                        "Model not found for project: " + command.projectId()));

        if (model.getVersion() != command.version()) {
            throw new ModelConflictException(
                    "Version mismatch: client sent " + command.version()
                    + " but current version is " + model.getVersion()
                    + ". Reload the model and retry.");
        }

        if (command.data() != null) {
            long delta = jsonBytes(command.data()) - jsonBytes(model.getData());
            applyQuotaDelta(command.requesterId(), delta);
        }

        model.updateData(command.data());
        return modelRepository.save(model);
    }

    private Project loadProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
    }

    private void assertOwner(Project project, UUID requesterId) {
        if (!project.getOwnerId().equals(requesterId)) {
            throw new ProjectOwnershipException("Only the project owner can modify its model.");
        }
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
