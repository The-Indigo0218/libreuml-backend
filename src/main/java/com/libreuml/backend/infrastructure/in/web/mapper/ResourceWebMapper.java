package com.libreuml.backend.infrastructure.in.web.mapper;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.resource.port.in.dto.CreateResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTagsResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTitleAndContentResourceCommand;
import com.libreuml.backend.domain.model.Resource;
import com.libreuml.backend.domain.model.ResourceType;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.CreateResourceRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.UpdateTagsRequest;
import com.libreuml.backend.infrastructure.in.web.dto.request.resource.UpdateTitleAndContentRequest;
import com.libreuml.backend.infrastructure.in.web.dto.response.resource.ResourceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ResourceWebMapper {

    @Mapping(target = "type", source = "request.type", qualifiedByName = "ValidateResourceType")
    @Mapping(target = "creatorId", source = "creatorId")
    CreateResourceCommand toCreateResourceCommand(CreateResourceRequest request, UUID creatorId);

    @Named("ValidateResourceType")
    default void validateResourceType(String type) {
        try {
            ResourceType resourceType = ResourceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Resource Type. Allowed: DOCUMENT, VIDEO, LINK");
        }
    }

    ResourceResponse toResponse(Resource resource);

    default PagedResult<ResourceResponse> toPagedResponse(PagedResult<Resource> result) {
        if (result == null) return null;

        List<ResourceResponse> mappedContent = result.content().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResult<>(
                mappedContent,
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.isLast()
        );
    }

    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "userId", source = "userId")
    UpdateTitleAndContentResourceCommand toUpdateTitleAndContentResourceCommand(UpdateTitleAndContentRequest request, UUID resourceId, UUID userId);

    @Mapping(target = "resourceId", source = "resourceId")
    @Mapping(target = "userId", source = "userId")
    UpdateTagsResourceCommand toUpdateTagsCommand(UpdateTagsRequest request, UUID resourceId, UUID userId);
}
