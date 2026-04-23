package com.libreuml.backend.application.projectmodel.port.service;

import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.projectmodel.dto.UpdateProjectModelCommand;
import com.libreuml.backend.application.projectmodel.exception.ModelQuotaExceededException;
import com.libreuml.backend.application.projectmodel.exception.ProjectModelConflictException;
import com.libreuml.backend.application.projectmodel.exception.ProjectModelNotFoundException;
import com.libreuml.backend.application.projectmodel.port.in.GetProjectModelUseCase;
import com.libreuml.backend.application.projectmodel.port.in.UpdateProjectModelUseCase;
import com.libreuml.backend.application.projectmodel.port.out.ProjectModelRepository;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectModel;
import com.libreuml.backend.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectModelService implements GetProjectModelUseCase, UpdateProjectModelUseCase {

    private final ProjectModelRepository projectModelRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProjectModel findByProjectId(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);

        return projectModelRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ProjectModelNotFoundException("Model not found for project: " + projectId));
    }

    @Override
    public ProjectModel update(UpdateProjectModelCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));
        project.assertOwner(command.requesterId());

        ProjectModel model = projectModelRepository.findByProjectId(command.projectId())
                .orElseThrow(() -> new ProjectModelNotFoundException("Model not found for project: " + command.projectId()));

        if (model.getVersion() != command.version()) {
            throw new ProjectModelConflictException(
                    "The model was modified by another session.",
                    model.getVersion(),
                    model.getModelData());
        }

        User user = userRepository.getUserById(command.requesterId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.requesterId()));

        long newModelBytes = command.data() != null
                ? command.data().toString().getBytes(StandardCharsets.UTF_8).length
                : 0L;
        long currentModelBytes = projectModelRepository.getModelDataBytesByProjectId(command.projectId());
        long delta = newModelBytes - currentModelBytes;

        if (delta > 0 && !user.hasQuotaFor(delta)) {
            long quota = user.getStorageQuotaBytes();
            long used = user.getStorageUsedBytes();
            throw new ModelQuotaExceededException(
                    "Storage quota exceeded.",
                    used + delta,
                    quota);
        }

        model.replaceData(command.data());
        ProjectModel saved = projectModelRepository.save(model);

        if (delta != 0) {
            if (delta > 0) {
                user.incrementUsage(delta);
            } else {
                user.decrementUsage(-delta);
            }
            userRepository.save(user);
        }

        projectRepository.touchUpdatedAt(command.projectId());

        return saved;
    }
}
