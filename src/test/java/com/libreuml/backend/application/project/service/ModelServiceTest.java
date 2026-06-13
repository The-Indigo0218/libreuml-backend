package com.libreuml.backend.application.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.application.project.dto.UpdateModelCommand;
import com.libreuml.backend.application.project.exception.ModelConflictException;
import com.libreuml.backend.application.project.exception.ModelNotFoundException;
import com.libreuml.backend.application.project.exception.ProjectNotFoundException;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.out.SemanticModelRepository;
import com.libreuml.backend.application.project.port.service.ModelService;
import com.libreuml.backend.application.user.port.out.UserRepository;
import com.libreuml.backend.domain.model.DiagramVisibility;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.SemanticModel;
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
class ModelServiceTest {

    @Mock private SemanticModelRepository modelRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private MetricsPort metricsPort;

    @InjectMocks private ModelService modelService;

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

    private ObjectNode data(String key, String value) {
        return mapper.createObjectNode().put(key, value);
    }

    @Test
    void findByProjectId_accessibleToOwner_returnsModel() {
        SemanticModel model = SemanticModel.builder().id(UUID.randomUUID()).projectId(projectId).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        assertEquals(model, modelService.findByProjectId(projectId, ownerId));
    }

    @Test
    void findByProjectId_strangerOnPrivateProject_throwsOwnership() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        assertThrows(ProjectOwnershipException.class,
                () -> modelService.findByProjectId(projectId, strangerId));
    }

    @Test
    void findByProjectId_projectMissing_throwsProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThrows(ProjectNotFoundException.class,
                () -> modelService.findByProjectId(projectId, ownerId));
    }

    @Test
    void update_byNonOwner_throwsOwnership() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        UpdateModelCommand cmd = new UpdateModelCommand(projectId, strangerId, data("k", "v"), 0);
        assertThrows(ProjectOwnershipException.class, () -> modelService.update(cmd));
        verify(modelRepository, never()).save(any());
    }

    @Test
    void update_versionMismatch_throwsConflict() {
        SemanticModel model = SemanticModel.builder().id(UUID.randomUUID()).projectId(projectId).version(5).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.of(model));

        UpdateModelCommand cmd = new UpdateModelCommand(projectId, ownerId, data("k", "v"), 4);
        assertThrows(ModelConflictException.class, () -> modelService.update(cmd));
        verify(modelRepository, never()).save(any());
    }

    @Test
    void update_modelMissing_throwsModelNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        UpdateModelCommand cmd = new UpdateModelCommand(projectId, ownerId, data("k", "v"), 0);
        assertThrows(ModelNotFoundException.class, () -> modelService.update(cmd));
    }

    @Test
    void update_validVersion_appliesDataAndChargesQuotaDelta() {
        SemanticModel model = SemanticModel.builder().id(UUID.randomUUID()).projectId(projectId).version(0).build();
        User owner = Student.builder().id(ownerId).emailVerifiedAt(Instant.now())
                .storageQuotaBytes(5_242_880L).storageUsedBytes(0L).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(modelRepository.findByProjectId(projectId)).thenReturn(Optional.of(model));
        when(userRepository.getUserById(ownerId)).thenReturn(Optional.of(owner));
        when(modelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ObjectNode payload = data("elements", "User");
        SemanticModel result = modelService.update(new UpdateModelCommand(projectId, ownerId, payload, 0));

        assertEquals(payload, result.getData());
        assertTrue(owner.getStorageUsedBytes() > 0); // delta charged
        verify(modelRepository).save(model);
    }
}
