package com.libreuml.backend.infrastructure.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.application.project.dto.UpdateProjectCommand;
import com.libreuml.backend.application.project.port.in.CreateProjectUseCase;
import com.libreuml.backend.application.project.port.in.DeleteProjectUseCase;
import com.libreuml.backend.application.project.port.in.GetProjectUseCase;
import com.libreuml.backend.application.project.port.in.UpdateProjectUseCase;
import com.libreuml.backend.application.projectdiagram.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.application.projectdiagram.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.application.projectdiagram.port.in.CreateProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.DeleteProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.GetProjectDiagramUseCase;
import com.libreuml.backend.application.projectdiagram.port.in.UpdateProjectDiagramUseCase;
import com.libreuml.backend.application.projectmodel.dto.UpdateProjectModelCommand;
import com.libreuml.backend.application.projectmodel.port.in.GetProjectModelUseCase;
import com.libreuml.backend.application.projectmodel.port.in.UpdateProjectModelUseCase;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.ProjectModel;
import com.libreuml.backend.infrastructure.in.web.dto.request.project.*;
import com.libreuml.backend.infrastructure.in.web.dto.response.project.*;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final GetProjectModelUseCase getProjectModelUseCase;
    private final UpdateProjectModelUseCase updateProjectModelUseCase;
    private final CreateProjectDiagramUseCase createProjectDiagramUseCase;
    private final GetProjectDiagramUseCase getProjectDiagramUseCase;
    private final UpdateProjectDiagramUseCase updateProjectDiagramUseCase;
    private final DeleteProjectDiagramUseCase deleteProjectDiagramUseCase;
    private final ObjectMapper objectMapper;

    // ── Projects ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<PagedResult<ProjectSummaryResponse>> listProjects(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<ProjectSummary> result = getProjectUseCase.listByOwner(principal.getId(), page, size);
        PagedResult<ProjectSummaryResponse> response = new PagedResult<>(
                result.content().stream().map(this::toSummaryResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProjectCreatedResponse> createProject(
            @RequestBody @Valid CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CreateProjectUseCase.CreatedProject result = createProjectUseCase.create(
                new CreateProjectCommand(
                        principal.getId(),
                        request.name(),
                        request.description(),
                        request.author(),
                        request.projectVersion(),
                        request.targetLanguage(),
                        request.basePackage()));

        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectCreatedResponse(
                result.project().getId(),
                result.model().getId(),
                result.project().getVersion(),
                result.project().getCreatedAt()));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Project project = getProjectUseCase.findById(projectId, principal.getId());
        List<ProjectDiagram> diagrams = getProjectDiagramUseCase.listByProject(projectId, principal.getId());
        return ResponseEntity.ok(toProjectResponse(project, diagrams));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectUpdatedResponse> updateProject(
            @PathVariable UUID projectId,
            @RequestBody @Valid UpdateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Project updated = updateProjectUseCase.update(new UpdateProjectCommand(
                projectId,
                principal.getId(),
                request.name(),
                request.description(),
                request.author(),
                request.projectVersion(),
                request.targetLanguage(),
                request.basePackage(),
                request.version()));

        return ResponseEntity.ok(new ProjectUpdatedResponse(
                updated.getId(), updated.getVersion(), updated.getUpdatedAt()));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        deleteProjectUseCase.delete(projectId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Full load ─────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/full")
    public ResponseEntity<ProjectFullResponse> getProjectFull(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        GetProjectUseCase.ProjectFull full = getProjectUseCase.findFull(projectId, principal.getId());
        return ResponseEntity.ok(toFullResponse(full));
    }

    // ── Project Model ─────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/model")
    public ResponseEntity<ProjectModelResponse> getModel(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectModel model = getProjectModelUseCase.findByProjectId(projectId, principal.getId());
        return ResponseEntity.ok(toModelResponse(model));
    }

    @PatchMapping("/{projectId}/model")
    public ResponseEntity<ProjectModelUpdatedResponse> updateModel(
            @PathVariable UUID projectId,
            @RequestBody @Valid UpdateProjectModelRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectModel updated = updateProjectModelUseCase.update(new UpdateProjectModelCommand(
                projectId,
                principal.getId(),
                request.data(),
                request.version()));

        return ResponseEntity.ok(new ProjectModelUpdatedResponse(
                updated.getId(), updated.getVersion(), updated.getUpdatedAt()));
    }

    // ── Diagrams ──────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/diagrams")
    public ResponseEntity<List<ProjectDiagramListItemResponse>> listDiagrams(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<ProjectDiagram> diagrams = getProjectDiagramUseCase.listByProject(projectId, principal.getId());
        return ResponseEntity.ok(diagrams.stream().map(this::toDiagramListItem).toList());
    }

    @PostMapping("/{projectId}/diagrams")
    public ResponseEntity<ProjectDiagramCreatedResponse> createDiagram(
            @PathVariable UUID projectId,
            @RequestBody @Valid CreateProjectDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ObjectNode viewData = request.viewData() != null
                ? request.viewData()
                : objectMapper.createObjectNode();

        ProjectDiagram diagram = createProjectDiagramUseCase.create(new CreateProjectDiagramCommand(
                projectId,
                principal.getId(),
                request.name(),
                request.diagramType(),
                request.path(),
                viewData));

        return ResponseEntity.status(HttpStatus.CREATED).body(new ProjectDiagramCreatedResponse(
                diagram.getId(), diagram.getProjectId(), diagram.getVersion(), diagram.getCreatedAt()));
    }

    @GetMapping("/{projectId}/diagrams/{diagramId}")
    public ResponseEntity<ProjectDiagramResponse> getDiagram(
            @PathVariable UUID projectId,
            @PathVariable UUID diagramId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectDiagram diagram = getProjectDiagramUseCase.findById(projectId, diagramId, principal.getId());
        return ResponseEntity.ok(toDiagramResponse(diagram));
    }

    @PatchMapping("/{projectId}/diagrams/{diagramId}")
    public ResponseEntity<ProjectDiagramUpdatedResponse> updateDiagram(
            @PathVariable UUID projectId,
            @PathVariable UUID diagramId,
            @RequestBody @Valid UpdateProjectDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectDiagram updated = updateProjectDiagramUseCase.update(new UpdateProjectDiagramCommand(
                projectId,
                diagramId,
                principal.getId(),
                request.name(),
                request.viewData(),
                request.version()));

        return ResponseEntity.ok(new ProjectDiagramUpdatedResponse(
                updated.getId(), updated.getVersion(), updated.getUpdatedAt()));
    }

    @DeleteMapping("/{projectId}/diagrams/{diagramId}")
    public ResponseEntity<Void> deleteDiagram(
            @PathVariable UUID projectId,
            @PathVariable UUID diagramId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        deleteProjectDiagramUseCase.delete(projectId, diagramId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private ProjectSummaryResponse toSummaryResponse(ProjectSummary s) {
        return new ProjectSummaryResponse(
                s.id(), s.name(), s.description(), s.author(), s.projectVersion(),
                s.targetLanguage(), s.basePackage(), s.visibility(), s.version(),
                s.diagramCount(), s.diagramTypes(), s.createdAt(), s.updatedAt());
    }

    private ProjectResponse toProjectResponse(Project p, List<ProjectDiagram> diagrams) {
        List<ProjectDiagramSummaryResponse> diagramSummaries = diagrams.stream()
                .map(d -> new ProjectDiagramSummaryResponse(
                        d.getId(), d.getName(), d.getDiagramType(), d.getPath(),
                        d.getVersion(), d.getUpdatedAt()))
                .toList();
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(), p.getAuthor(), p.getProjectVersion(),
                p.getTargetLanguage(), p.getBasePackage(), p.getVisibility(), p.getVersion(),
                diagramSummaries, p.getCreatedAt(), p.getUpdatedAt());
    }

    private ProjectModelResponse toModelResponse(ProjectModel m) {
        return new ProjectModelResponse(
                m.getId(), m.getProjectId(), m.getModelData(), m.getVersion(), m.getUpdatedAt());
    }

    private ProjectDiagramListItemResponse toDiagramListItem(ProjectDiagram d) {
        return new ProjectDiagramListItemResponse(
                d.getId(), d.getProjectId(), d.getName(), d.getDiagramType(),
                d.getPath(), d.getVersion(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private ProjectDiagramResponse toDiagramResponse(ProjectDiagram d) {
        return new ProjectDiagramResponse(
                d.getId(), d.getProjectId(), d.getName(), d.getDiagramType(),
                d.getPath(), d.getViewData(), d.getVersion(), d.getCreatedAt(), d.getUpdatedAt());
    }

    private ProjectFullResponse toFullResponse(GetProjectUseCase.ProjectFull full) {
        Project p = full.project();
        ProjectModel m = full.model();

        ProjectFullResponse.ProjectData projectData = new ProjectFullResponse.ProjectData(
                p.getId(), p.getName(), p.getDescription(), p.getAuthor(), p.getProjectVersion(),
                p.getTargetLanguage(), p.getBasePackage(), p.getVisibility(), p.getVersion(),
                p.getCreatedAt(), p.getUpdatedAt());

        ProjectFullResponse.ModelData modelData = new ProjectFullResponse.ModelData(
                m.getId(), m.getModelData(), m.getVersion(), m.getUpdatedAt());

        List<ProjectFullResponse.DiagramData> diagramData = full.diagrams().stream()
                .map(d -> new ProjectFullResponse.DiagramData(
                        d.getId(), d.getName(), d.getDiagramType(), d.getPath(),
                        d.getViewData(), d.getVersion(), d.getCreatedAt(), d.getUpdatedAt()))
                .toList();

        return new ProjectFullResponse(projectData, modelData, diagramData);
    }
}
