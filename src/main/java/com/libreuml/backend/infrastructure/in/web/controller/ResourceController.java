package com.libreuml.backend.infrastructure.in.web.controller;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.application.resource.port.in.dto.*;
import com.libreuml.backend.application.resource.port.service.ResourceService;
import com.libreuml.backend.domain.model.Resource;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.CreateResourceRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.UpdateTagsRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.UpdateTitleAndContentRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.resource.ResourceResponse;
import com.libreuml.backend.infrastructure.in.web.mapper.ResourceWebMapper;
import com.libreuml.backend.infrastructure.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceWebMapper resourceWebMapper;

    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(
            @RequestBody @Valid CreateResourceRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CreateResourceCommand command = resourceWebMapper.toCreateResourceCommand(request, userDetails.getId());
        ResourceResponse response = resourceWebMapper.toResponse(resourceService.create(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateTitleAndContent(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTitleAndContentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateTitleAndContentResourceCommand command = resourceWebMapper.toUpdateTitleAndContentResourceCommand(request, id, userDetails.getId());
        ResourceResponse response = resourceWebMapper.toResponse(resourceService.updateTitleAndContent(command));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateResource(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DeactivateResourceCommand command = new DeactivateResourceCommand(id, userDetails.getId());
        resourceService.deactivateResource(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PagedResult<ResourceResponse>> getAllResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String tag
    ) {
        PaginationCommand pagination = new PaginationCommand(page, size, sortBy, direction);

        PagedResult<Resource> resources;

        if (title != null && !title.isBlank()) {
            resources = resourceService.getResourceByTitle(new GetResourceByTitleCommand(title, pagination));
        } else if (tag != null && !tag.isBlank()) {
            resources = resourceService.getResourceByTag(new GetResourceByTagCommand(tag, pagination));
        } else {
            resources = resourceService.getResourceByTitle(new GetResourceByTitleCommand("", pagination));
        }
        return ResponseEntity.ok(resourceWebMapper.toPagedResponse(resources));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable UUID id) {
        Resource resource = resourceService.getResourceById(new GetResourceByIdCommand(id));
        return ResponseEntity.ok(resourceWebMapper.toResponse(resource));
    }


    @PatchMapping("/{id}/tags")
    public ResponseEntity<ResourceResponse> updateTags(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTagsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UpdateTagsResourceCommand command = resourceWebMapper.toUpdateTagsCommand(request, id, userDetails.getId());
        Resource updatedResource = resourceService.updateResourceTags(command);
        return ResponseEntity.ok(resourceWebMapper.toResponse(updatedResource));
    }
}