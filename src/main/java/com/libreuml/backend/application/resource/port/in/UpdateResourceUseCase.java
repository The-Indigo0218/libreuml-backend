package com.libreuml.backend.application.resource.port.in;

import com.libreuml.backend.application.resource.port.in.dto.DeactivateResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTagsResourceCommand;
import com.libreuml.backend.application.resource.port.in.dto.UpdateTitleAndContentResourceCommand;
import com.libreuml.backend.domain.model.Resource;

public interface UpdateResourceUseCase {
    Resource updateTitleAndContent(UpdateTitleAndContentResourceCommand command );
    Resource deactivateResource(DeactivateResourceCommand command);
    Resource updateResourceTags(UpdateTagsResourceCommand command);
}
