package com.libreuml.backend.application.projectdiagram.port.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.audit.port.out.AuditLogPort;
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
import com.libreuml.backend.application.emailverification.exception.EmailNotVerifiedException;
import com.libreuml.backend.application.projectmodel.exception.ModelQuotaExceededException;
import com.libreuml.backend.application.user.exception.UserNotFoundException;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.AuditEventType;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.DiagramPayloadTooLargeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectDiagramService implements CreateProjectDiagramUseCase, GetProjectDiagramUseCase,
        UpdateProjectDiagramUseCase, DeleteProjectDiagramUseCase {

    private static final long MAX_VIEW_DATA_BYTES = 5_242_880L; // 5 MB

    private final ProjectDiagramRepository projectDiagramRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogPort auditLogPort;

    @Override
    public ProjectDiagram create(CreateProjectDiagramCommand command) {
        Project project = projectRepository.findById(command.projectId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + command.projectId()));
        project.assertOwner(command.requesterId());

        long viewDataBytes = sizeOf(command.viewData());
        assertPayloadSize(viewDataBytes);

        User user = userRepository.getUserById(command.requesterId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.requesterId()));
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email verification required to create cloud diagrams.");
        }

        if (!user.hasQuotaFor(viewDataBytes)) {
            throw new ModelQuotaExceededException(
                    "Storage quota exceeded.",
                    user.getStorageUsedBytes() + viewDataBytes,
                    user.getStorageQuotaBytes());
        }

        ProjectDiagram diagram = ProjectDiagram.create(
                command.projectId(),
                command.name(),
                command.diagramType(),
                command.path(),
                command.viewData()
        );
        ProjectDiagram saved = projectDiagramRepository.save(diagram);

        if (viewDataBytes > 0) {
            user.incrementUsage(viewDataBytes);
            userRepository.save(user);
        }

        auditLogPort.log(AuditEventType.DIAGRAM_CREATED, command.requesterId(), null, null,
                "{\"projectId\":\"" + command.projectId() + "\",\"diagramId\":\"" + saved.getId() + "\"}");

        return saved;
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

        long newBytes = sizeOf(command.viewData());
        assertPayloadSize(newBytes);

        User user = userRepository.getUserById(command.requesterId())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + command.requesterId()));
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email verification required to update cloud diagrams.");
        }

        long currentBytes = sizeOf(diagram.getViewData());
        long delta = newBytes - currentBytes;

        if (delta > 0) {
            if (!user.hasQuotaFor(delta)) {
                throw new ModelQuotaExceededException(
                        "Storage quota exceeded.",
                        user.getStorageUsedBytes() + delta,
                        user.getStorageQuotaBytes());
            }
            user.incrementUsage(delta);
            userRepository.save(user);
        } else if (delta < 0) {
            user.decrementUsage(-delta);
            userRepository.save(user);
        }

        diagram.update(command.name(), command.viewData());
        ProjectDiagram saved = projectDiagramRepository.save(diagram);

        projectRepository.touchUpdatedAt(command.projectId());

        auditLogPort.log(AuditEventType.DIAGRAM_UPDATED, command.requesterId(), null, null,
                "{\"projectId\":\"" + command.projectId() + "\",\"diagramId\":\"" + command.diagramId() + "\"}");

        return saved;
    }

    @Override
    public void delete(UUID projectId, UUID diagramId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));
        project.assertOwner(requesterId);

        ProjectDiagram diagram = projectDiagramRepository.findByProjectIdAndId(projectId, diagramId)
                .orElseThrow(() -> new ProjectDiagramNotFoundException("Diagram not found: " + diagramId));

        long freedBytes = sizeOf(diagram.getViewData());
        projectDiagramRepository.deleteById(diagramId);

        if (freedBytes > 0) {
            userRepository.getUserById(requesterId).ifPresent(user -> {
                user.decrementUsage(freedBytes);
                userRepository.save(user);
            });
        }

        auditLogPort.log(AuditEventType.DIAGRAM_DELETED, requesterId, null, null,
                "{\"projectId\":\"" + projectId + "\",\"diagramId\":\"" + diagramId + "\"}");
    }

    private static long sizeOf(ObjectNode node) {
        if (node == null) return 0L;
        return node.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private static void assertPayloadSize(long bytes) {
        if (bytes > MAX_VIEW_DATA_BYTES) {
            throw new DiagramPayloadTooLargeException(
                    "Diagram view data exceeds the 5 MB limit (" + bytes + " bytes received).");
        }
    }
}
