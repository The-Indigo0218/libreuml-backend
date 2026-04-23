package com.libreuml.backend.application.project.service;

import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.port.in.CreateProjectUseCase;
import com.libreuml.backend.application.project.port.out.ProjectRepository;
import com.libreuml.backend.application.project.port.service.ProjectService;
import com.libreuml.backend.application.projectdiagram.port.out.ProjectDiagramRepository;
import com.libreuml.backend.application.projectmodel.port.out.ProjectModelRepository;
import com.libreuml.backend.domain.model.ProjectKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectModelRepository projectModelRepository;
    @Mock
    private ProjectDiagramRepository projectDiagramRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("Should default projectKind to FREE when command carries null")
    void create_withNullProjectKind_defaultsToFree() {
        CreateProjectCommand cmd = new CreateProjectCommand(
                UUID.randomUUID(), "My Project", null, null, null, null, null, null);

        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectUseCase.CreatedProject result = projectService.create(cmd);

        assertEquals(ProjectKind.FREE, result.project().getProjectKind());
    }

    @Test
    @DisplayName("Should preserve SOFTWARE_ARCHITECTURE kind when explicitly set")
    void create_withSoftwareArchitectureKind_preservesKind() {
        CreateProjectCommand cmd = new CreateProjectCommand(
                UUID.randomUUID(), "Arch Project", null, null, null, null, null,
                ProjectKind.SOFTWARE_ARCHITECTURE);

        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectUseCase.CreatedProject result = projectService.create(cmd);

        assertEquals(ProjectKind.SOFTWARE_ARCHITECTURE, result.project().getProjectKind());
    }

    @Test
    @DisplayName("Should preserve FREE kind when explicitly set")
    void create_withFreeKind_preservesKind() {
        CreateProjectCommand cmd = new CreateProjectCommand(
                UUID.randomUUID(), "Free Project", null, null, null, null, null,
                ProjectKind.FREE);

        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectUseCase.CreatedProject result = projectService.create(cmd);

        assertEquals(ProjectKind.FREE, result.project().getProjectKind());
    }

    @Test
    @DisplayName("Should return both saved project and model on create")
    void create_returnsProjectAndModel() {
        UUID ownerId = UUID.randomUUID();
        CreateProjectCommand cmd = new CreateProjectCommand(
                ownerId, "Full Project", "desc", "Author", "1.0.0", "Java", "com.example",
                ProjectKind.FREE);

        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projectModelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateProjectUseCase.CreatedProject result = projectService.create(cmd);

        assertNotNull(result.project());
        assertNotNull(result.model());
        assertEquals(ownerId, result.project().getOwnerId());
        verify(projectRepository).save(any());
        verify(projectModelRepository).save(any());
    }
}
