package com.libreuml.backend.infrastructure.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.diagram.dto.CreateDiagramCommand;
import com.libreuml.backend.application.diagram.dto.UpdateDiagramCommand;
import com.libreuml.backend.application.diagram.port.in.CreateDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.DeleteDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.GetDiagramUseCase;
import com.libreuml.backend.application.diagram.port.in.UpdateDiagramUseCase;
import com.libreuml.backend.domain.model.Diagram;
import com.libreuml.backend.infrastructure.in.web.dto.request.diagram.CreateDiagramRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.diagram.UpdateDiagramRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.diagram.DiagramResponse;
import com.libreuml.backend.infrastructure.in.web.dto.response.diagram.DiagramSummaryResponse;
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

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/diagrams")
@RequiredArgsConstructor
public class DiagramController {

    private final CreateDiagramUseCase createDiagramUseCase;
    private final GetDiagramUseCase getDiagramUseCase;
    private final UpdateDiagramUseCase updateDiagramUseCase;
    private final DeleteDiagramUseCase deleteDiagramUseCase;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<DiagramResponse> create(
            @RequestBody @Valid CreateDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ObjectNode content = request.content() != null
                ? request.content()
                : objectMapper.createObjectNode();

        Diagram diagram = createDiagramUseCase.create(
                new CreateDiagramCommand(principal.getId(), request.title(), request.type(), content));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(diagram));
    }

    @GetMapping
    public ResponseEntity<PagedResult<DiagramSummaryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        PagedResult<Diagram> result = getDiagramUseCase.listByOwner(principal.getId(), page, size);
        PagedResult<DiagramSummaryResponse> response = new PagedResult<>(
                result.content().stream().map(this::toSummaryResponse).toList(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagramResponse> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Diagram diagram = getDiagramUseCase.findById(id, principal.getId());
        return ResponseEntity.ok(toResponse(diagram));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DiagramResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDiagramRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Diagram diagram = updateDiagramUseCase.update(
                new UpdateDiagramCommand(
                        id, principal.getId(),
                        request.title(), request.content(),
                        request.version()));

        return ResponseEntity.ok(toResponse(diagram));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        deleteDiagramUseCase.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private DiagramResponse toResponse(Diagram diagram) {
        return new DiagramResponse(
                diagram.getId(),
                diagram.getOwnerId(),
                diagram.getTitle(),
                diagram.getType(),
                diagram.getVisibility(),
                diagram.getContent(),
                diagram.getVersion(),
                diagram.getCreatedAt(),
                diagram.getUpdatedAt());
    }

    private DiagramSummaryResponse toSummaryResponse(Diagram diagram) {
        return new DiagramSummaryResponse(
                diagram.getId(),
                diagram.getTitle(),
                diagram.getType(),
                diagram.getVisibility(),
                diagram.getVersion(),
                diagram.getCreatedAt(),
                diagram.getUpdatedAt());
    }
}
