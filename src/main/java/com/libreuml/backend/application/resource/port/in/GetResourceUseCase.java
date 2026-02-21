package com.libreuml.backend.application.resource.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.resource.port.in.dto.GetResourceByCreatorIdCommand;
import com.libreuml.backend.application.resource.port.in.dto.GetResourceByIdCommand;
import com.libreuml.backend.application.resource.port.in.dto.GetResourceByTagCommand;
import com.libreuml.backend.application.resource.port.in.dto.GetResourceByTitleCommand;
import com.libreuml.backend.domain.model.Resource;

public interface GetResourceUseCase {
    Resource getResourceById(GetResourceByIdCommand command);
    PagedResult<Resource> getResourceByTitle(GetResourceByTitleCommand command);
    PagedResult<Resource> getResourceByTag(GetResourceByTagCommand command);
    PagedResult<Resource> getResourceByCreator(GetResourceByCreatorIdCommand command);
    Long countResourceByTag(String tag);
    Long totalActiveResources();
}
