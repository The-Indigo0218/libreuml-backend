package com.libreuml.backend.application.project.service;

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
import com.libreuml.backend.application.project.exception.ProjectConflictException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.out.SemanticModelRepository;
import com.libreuml.backend.application.project.port.service.ProjectService;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectDiagramType;
import com.libreuml.backend.domain.model.ProjectKind;
import com.libreuml.backend.domain.model.SemanticModel;
import com.libreuml.backend.domain.model.Student;
import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.domain.model.exception.ProjectOwnershipException;
import com.libreuml.backend.domain.model.exception.QuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private SemanticModelRepository modelRepository;
    @Mock private ProjectDiagramRepository projectDiagramRepository;
    @Mock private UserRepository userRepository;
    @Mock private MetricsPort metricsPort;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private ProjectService projectService;

    private UUID ownerId;
    private UUID strangerId;
    private User verifiedOwner;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        strangerId = UUID.randomUUID();
        verifiedOwner = Student.builder()
                .id(ownerId)
                .emailVerifiedAt(Instant.now())
                .storageQuotaBytes(5_242_880L)
                .storageUsedBytes(0L)
                .build();
    }

    private ObjectNode node(String key, String value) {
        return objectMapper.createObjectNode().put(key, value);
    }

    private CreateProjectCommand createCommand(ObjectNode snapshot) {
        return new CreateProjectCommand(ownerId, "Demo", "desc", "author",
                "1.0.0", ProjectKind.FREE, "Java", "com.demo", snapshot);
    }

    @Test
    void create_withVerifiedUser_persistsProjectAndEmptyModel_andChargesQuota() {
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(verifiedOwner));
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            return Project.builder().id(UUID.randomUUID()).ownerId(p.getOwnerId())
                    .name(p.getName()).projectKind(p.getProjectKind())
                    .visibility(p.getVisibility()).vfsSnapshot(p.getVfsSnapshot())
                    .version(0).createdAt(Instant.now()).build();
        });
        UUID modelId = UUID.randomUUID();
        when(modelRepository.save(any())).thenReturn(
                SemanticModel.builder().id(modelId).projectId(UUID.randomUUID()).version(0).build());

        CreateProjectResult result = projectService.create(createCommand(node("k", "v")));

        assertNotNull(result.project().getId());
        assertEquals(modelId, result.modelId());
        verify(modelRepository).save(any());            // empty model created alongside
        verify(userRepository).save(verifiedOwner);     // quota charged
        verify(metricsPort).incrementProjectSaved(ProjectKind.FREE);
        assertTrue(verifiedOwner.getStorageUsedBytes() > 0);
    }

    @Test
    void create_withUnverifiedUser_throwsEmailNotVerified() {
        User unverified = Student.builder().id(ownerId).build();
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(unverified));

        assertThrows(EmailNotVerifiedException.class,
                () -> projectService.create(createCommand(node("k", "v"))));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void create_overQuota_throwsQuotaExceeded() {
        verifiedOwner.setStorageUsedBytes(5_242_880L); // full
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(verifiedOwner));

        assertThrows(QuotaExceededException.class,
                () -> projectService.create(createCommand(node("k", "v"))));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void findById_notFound_throwsProjectNotFound() {
        UUID id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ProjectNotFoundException.class, () -> projectService.findById(id, ownerId));
    }

    @Test
    void findById_strangerOnPrivateProject_throwsOwnership() {
        Project privateProject = Project.builder().id(UUID.randomUUID()).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).build();
        when(projectRepository.findById(privateProject.getId())).thenReturn(Optional.of(privateProject));

        assertThrows(ProjectOwnershipException.class,
                () -> projectService.findById(privateProject.getId(), strangerId));
    }

    @Test
    void getFull_returnsProjectModelAndDiagrams() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).build();
        SemanticModel model = SemanticModel.builder().id(UUID.randomUUID()).projectId(projectId).build();
        ProjectDiagram diagram = ProjectDiagram.builder().id(UUID.randomUUID()).projectId(projectId).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(projectDiagramRepository.findAllByProjectId(projectId)).thenReturn(List.of(diagram));

        ProjectFull full = projectService.getFull(projectId, ownerId);

        assertEquals(project, full.project());
        assertEquals(model, full.model());
        assertEquals(1, full.diagrams().size());
    }

    @Test
    void update_versionMismatch_throwsConflict() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).version(3).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        UpdateProjectCommand cmd = new UpdateProjectCommand(projectId, ownerId,
                "New name", null, null, null, null, null, 2); // stale version
        assertThrows(ProjectConflictException.class, () -> projectService.update(cmd));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void update_validVersion_appliesChangesAndSaves() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId)
                .name("Old").visibility(DiagramVisibility.PRIVATE).version(1).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateProjectCommand cmd = new UpdateProjectCommand(projectId, ownerId,
                "New name", null, null, null, null, null, 1);
        Project updated = projectService.update(cmd);

        assertEquals("New name", updated.getName());
        verify(projectRepository).save(project);
    }

    @Test
    void delete_strangerNotOwner_throwsOwnership() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(ProjectOwnershipException.class,
                () -> projectService.delete(projectId, strangerId));
        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void delete_owner_reclaimsQuotaAndDeletes() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId)
                .visibility(DiagramVisibility.PRIVATE).vfsSnapshot(node("k", "v")).build();
        verifiedOwner.setStorageUsedBytes(1_000L);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(projectDiagramRepository.findAllByProjectId(projectId)).thenReturn(List.of());
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(verifiedOwner));

        projectService.delete(projectId, ownerId);

        verify(projectRepository).deleteById(projectId);
        verify(userRepository).save(verifiedOwner);
        assertTrue(verifiedOwner.getStorageUsedBytes() < 1_000L); // snapshot bytes reclaimed
    }

    @Test
    void listByOwner_enrichesEachProjectWithDiagramCountAndTypes() {
        UUID projectId = UUID.randomUUID();
        Project project = Project.builder().id(projectId).ownerId(ownerId).build();
        when(projectRepository.findAllByOwnerId(ownerId, 0, 20)).thenReturn(
                new PagedResult<>(List.of(project), 0, 20, 1, 1, true));
        when(projectDiagramRepository.findDiagramTypesByProjectIds(List.of(projectId))).thenReturn(
                Map.of(projectId, List.of(ProjectDiagramType.CLASS, ProjectDiagramType.CLASS,
                        ProjectDiagramType.DOMAIN)));

        PagedResult<ProjectSummary> result = projectService.listByOwner(ownerId, 0, 20);

        ProjectSummary summary = result.content().get(0);
        assertEquals(3, summary.diagramCount());
        assertEquals(List.of(ProjectDiagramType.CLASS, ProjectDiagramType.DOMAIN), summary.diagramTypes());
    }
}
