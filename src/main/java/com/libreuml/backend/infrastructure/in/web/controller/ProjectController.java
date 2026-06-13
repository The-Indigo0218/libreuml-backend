package com.libreuml.backend.infrastructure.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.project.dto.CreateProjectCommand;
import com.libreuml.backend.application.project.dto.CreateProjectDiagramCommand;
import com.libreuml.backend.application.project.dto.CreateProjectResult;
import com.libreuml.backend.application.project.dto.ProjectFull;
import com.libreuml.backend.application.project.dto.ProjectSummary;
import com.libreuml.backend.application.project.dto.UpdateModelCommand;
import com.libreuml.backend.application.project.dto.UpdateProjectCommand;
import com.libreuml.backend.application.project.dto.UpdateProjectDiagramCommand;
import com.libreuml.backend.application.project.port.in.*;
import com.libreuml.backend.domain.model.Project;
import com.libreuml.backend.domain.model.ProjectDiagram;
import com.libreuml.backend.domain.model.SemanticModel;
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

/**
 * Project-centric cloud-sync API the modeler frontend drives: a project owns one semantic model
 * and N diagrams. All routes require authentication; ownership is enforced in the application layer.
 */
@Validated
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final GetModelUseCase getModelUseCase;
    private final UpdateModelUseCase updateModelUseCase;
    private final CreateProjectDiagramUseCase createProjectDiagramUseCase;
    private final GetProjectDiagramUseCase getProjectDiagramUseCase;
    private final UpdateProjectDiagramUseCase updateProjectDiagramUseCase;
    private final DeleteProjectDiagramUseCase deleteProjectDiagramUseCase;
    private final ObjectMapper objectMapper;

    // ── Projects ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CreateProjectResponse> create(
            @RequestBody @Valid CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        CreateProjectResult result = createProjectUseCase.create(new CreateProjectCommand(
                principal.getId(), request.name(), request.description(), request.author(),
                request.projectVersion(), request.projectKind(),
                request.targetLanguage(), request.basePackage(), request.vfsSnapshot()));

        Project project = result.project();
        CreateProjectResponse body = new CreateProjectResponse(
                project.getId(), result.modelId(), project.getVersion(), project.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<PagedResult<ProjectSummaryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<ProjectSummary> result = getProjectUseCase.listByOwner(principal.getId(), page, size);
        PagedResult<ProjectSummaryResponse> response = new PagedResult<>(
                result.content().stream().map(this::toSummaryResponse).toList(),
                result.pageNumber(), result.pageSize(),
                result.totalElements(), result.totalPages(), result.isLast());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Project project = getProjectUseCase.findById(id, principal.getId());
        List<ProjectDiagram> diagrams = getProjectDiagramUseCase.listByProject(id, principal.getId());
        return ResponseEntity.ok(toDetailResponse(project, diagrams));
    }

    @GetMapping("/{id}/full")
    public ResponseEntity<ProjectFullResponse> getFull(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectFull full = getProjectUseCase.getFull(id, principal.getId());
        return ResponseEntity.ok(new ProjectFullResponse(
                toDetailResponse(full.project(), full.diagrams()),
                toModelResponse(full.model()),
                full.diagrams().stream().map(this::toCloudDiagramResponse).toList()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UpdateProjectResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Project project = updateProjectUseCase.update(new UpdateProjectCommand(
                id, principal.getId(), request.name(), request.description(), request.author(),
                request.targetLanguage(), request.basePackage(), request.vfsSnapshot(), request.version()));

        return ResponseEntity.ok(new UpdateProjectResponse(
                project.getId(), project.getVersion(), project.getUpdatedAt()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        deleteProjectUseCase.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/model")
    public ResponseEntity<ModelResponse> getModel(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        SemanticModel model = getModelUseCase.findByProjectId(projectId, principal.getId());
        return ResponseEntity.ok(toModelResponse(model));
    }

    @PatchMapping("/{projectId}/model")
    public ResponseEntity<UpdateModelResponse> updateModel(
            @PathVariable UUID projectId,
            @RequestBody @Valid UpdateModelRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        SemanticModel model = updateModelUseCase.update(new UpdateModelCommand(
                projectId, principal.getId(), request.data(), request.version()));
        return ResponseEntity.ok(new UpdateModelResponse(
                model.getId(), model.getVersion(), model.getUpdatedAt()));
    }

    // ── Diagrams ────────────────────────────────────────────────────────────────

    @GetMapping("/{projectId}/diagrams")
    public ResponseEntity<List<CloudDiagramResponse>> listDiagrams(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        List<ProjectDiagram> diagrams = getProjectDiagramUseCase.listByProject(projectId, principal.getId());
        return ResponseEntity.ok(diagrams.stream().map(this::toCloudDiagramResponse).toList());
    }

    @PostMapping("/{projectId}/diagrams")
    public ResponseEntity<CreateCloudDiagramResponse> createDiagram(
            @PathVariable UUID projectId,
            @RequestBody @Valid CreateProjectDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ObjectNode viewData = request.viewData() != null ? request.viewData() : objectMapper.createObjectNode();
        ProjectDiagram diagram = createProjectDiagramUseCase.create(new CreateProjectDiagramCommand(
                projectId, principal.getId(), request.name(), request.diagramType(), request.path(), viewData));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateCloudDiagramResponse(
                diagram.getId(), diagram.getProjectId(), diagram.getVersion(), diagram.getCreatedAt()));
    }

    @GetMapping("/{projectId}/diagrams/{diagramId}")
    public ResponseEntity<CloudDiagramResponse> getDiagram(
            @PathVariable UUID projectId,
            @PathVariable UUID diagramId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectDiagram diagram = getProjectDiagramUseCase.findById(projectId, diagramId, principal.getId());
        return ResponseEntity.ok(toCloudDiagramResponse(diagram));
    }

    @PatchMapping("/{projectId}/diagrams/{diagramId}")
    public ResponseEntity<UpdateCloudDiagramResponse> updateDiagram(
            @PathVariable UUID projectId,
            @PathVariable UUID diagramId,
            @RequestBody @Valid UpdateProjectDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ProjectDiagram diagram = updateProjectDiagramUseCase.update(new UpdateProjectDiagramCommand(
                projectId, diagramId, principal.getId(), request.name(), request.viewData(), request.version()));
        return ResponseEntity.ok(new UpdateCloudDiagramResponse(
                diagram.getId(), diagram.getVersion(), diagram.getUpdatedAt()));
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

    private ProjectSummaryResponse toSummaryResponse(ProjectSummary summary) {
        Project p = summary.project();
        return new ProjectSummaryResponse(
                p.getId(), p.getName(), p.getDescription(), p.getAuthor(), p.getProjectVersion(),
                p.getProjectKind(), p.getTargetLanguage(), p.getBasePackage(), p.getVisibility(),
                p.getVersion(), summary.diagramCount(), summary.diagramTypes(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private ProjectDetailResponse toDetailResponse(Project p, List<ProjectDiagram> diagrams) {
        return new ProjectDetailResponse(
                p.getId(), p.getName(), p.getDescription(), p.getAuthor(), p.getProjectVersion(),
                p.getProjectKind(), p.getTargetLanguage(), p.getBasePackage(), p.getVisibility(),
                p.getVersion(), p.getVfsSnapshot(),
                diagrams.stream().map(this::toDiagramSummary).toList(),
                p.getCreatedAt(), p.getUpdatedAt());
    }

    private ProjectDiagramSummaryResponse toDiagramSummary(ProjectDiagram d) {
        return new ProjectDiagramSummaryResponse(
                d.getId(), d.getName(), d.getDiagramType(), d.getPath(), d.getVersion(), d.getUpdatedAt());
    }

    private ModelResponse toModelResponse(SemanticModel m) {
        return new ModelResponse(m.getId(), m.getProjectId(), m.getData(), m.getVersion(), m.getUpdatedAt());
    }

    private CloudDiagramResponse toCloudDiagramResponse(ProjectDiagram d) {
        return new CloudDiagramResponse(
                d.getId(), d.getProjectId(), d.getName(), d.getDiagramType(), d.getPath(),
                d.getViewData(), d.getVersion(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
