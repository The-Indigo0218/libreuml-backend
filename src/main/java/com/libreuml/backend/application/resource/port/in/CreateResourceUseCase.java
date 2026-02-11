package com.libreuml.backend.application.resource.port.in;

import com.libreuml.backend.application.resource.port.in.dto.CreateResourceCommand;
import com.libreuml.backend.domain.model.Resource;

public interface CreateResourceUseCase {
    Resource create(CreateResourceCommand command);
}
