package com.libreuml.backend.application.resource.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.Resource;

import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository {
    Resource save(Resource resource);
    Optional<Resource> findById(UUID id);
    PagedResult<Resource> findAllByCreatorId(UUID creatorId, PaginationCommand pagination);
}