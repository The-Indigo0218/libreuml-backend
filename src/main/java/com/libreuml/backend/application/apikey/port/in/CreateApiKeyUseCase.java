package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.apikey.dto.CreateApiKeyCommand;
import com.libreuml.backend.application.apikey.dto.CreatedApiKeyResult;

public interface CreateApiKeyUseCase {
    /** Creates a USER key for the authenticated user. The plain token is returned exactly once. */
    CreatedApiKeyResult createUserKey(CreateApiKeyCommand command);
}
