package com.libreuml.backend.application.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.project.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.application.project.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.application.project.exception.ProjectDiagramConflictException;
import com.libreuml.backend.application.project.exception.ProjectDiagramNotFoundException;
import com.libreuml.backend.application.project.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.service.ProjectDiagramService;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectDiagramType;
import com.libreuml.backend.domain.model.Student;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectDiagramServiceTest {

    @Mock private ProjectDiagramRepository projectDiagramRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private MetricsPort metricsPort;

    @InjectMocks private ProjectDiagramService service;

    private final ObjectMapper mapper = new ObjectMapper();
    private UUID ownerId;
    private UUID strangerId;
    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        project = Project.builder().id(projectId).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).build();
    }

    private ObjectNode view() {
        return mapper.createObjectNode().put("nodes", "[]");
    }

    @Test
    void create_byOwner_savesAndChargesQuota() {
        User owner = Student.builder().id(ownerId).emailVerifiedAt(Instant.now())
                .storageQuotaBytes(5_242_880L).storageUsedBytes(0L).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(owner));
        when(projectDiagramRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectDiagramCommand cmd = new CreateProjectDiagramCommand(
                projectId, ownerId, "Domain", ProjectDiagramType.DOMAIN, "vfs-1", view());
        ProjectDiagram created = service.create(cmd);

        assertEquals("Domain", created.getName());
        assertEquals(ProjectDiagramType.DOMAIN, created.getDiagramType());
        assertTrue(owner.getStorageUsedBytes() > 0);
        verify(projectDiagramRepository).save(any());
    }

    @Test
    void create_byNonOwner_throwsOwnership() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        CreateProjectDiagramCommand cmd = new CreateProjectDiagramCommand(
                projectId, strangerId, "Domain", ProjectDiagramType.DOMAIN, "vfs-1", view());
        assertThrows(ProjectOwnershipException.class, () -> service.create(cmd));
        verify(projectDiagramRepository, never()).save(any());
    }

    @Test
    void findById_diagramBelongsToAnotherProject_throwsNotFound() {
        UUID diagramId = UUID.randomUUID();
        ProjectDiagram foreign = ProjectDiagram.builder().id(diagramId)
                .projectId(UUID.randomUUID()).build(); // different project
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectDiagramRepository.findById(diagramId)).thenReturn(Optional.of(foreign));

        assertThrows(ProjectDiagramNotFoundException.class,
                () -> service.findById(projectId, diagramId, ownerId));
    }

    @Test
    void update_versionMismatch_throwsConflict() {
        UUID diagramId = UUID.randomUUID();
        ProjectDiagram diagram = ProjectDiagram.builder().id(diagramId).projectId(projectId).version(2).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectDiagramRepository.findById(diagramId)).thenReturn(Optional.of(diagram));

        UpdateProjectDiagramCommand cmd = new UpdateProjectDiagramCommand(
                projectId, diagramId, ownerId, "New", view(), 1); // stale
        assertThrows(ProjectDiagramConflictException.class, () -> service.update(cmd));
        verify(projectDiagramRepository, never()).save(any());
    }

    @Test
    void listByProject_strangerOnPrivateProject_throwsOwnership() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        assertThrows(ProjectOwnershipException.class,
                () -> service.listByProject(projectId, strangerId));
    }

    @Test
    void delete_byOwner_reclaimsQuotaAndDeletes() {
        UUID diagramId = UUID.randomUUID();
        ProjectDiagram diagram = ProjectDiagram.builder().id(diagramId).projectId(projectId)
                .viewData(view()).build();
        User owner = Student.builder().id(ownerId).storageQuotaBytes(5_242_880L).storageUsedBytes(500L).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectDiagramRepository.findById(diagramId)).thenReturn(Optional.of(diagram));
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(owner));

        service.delete(projectId, diagramId, ownerId);

        verify(projectDiagramRepository).deleteById(diagramId);
        verify(userRepository).save(owner);
        assertTrue(owner.getStorageUsedBytes() < 500L);
    }
}
