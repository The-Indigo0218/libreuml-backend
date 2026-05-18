package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.apikey.dto.CreatePartnerKeyCommand;
import com.libreuml.backend.application.apikey.dto.CreatedApiKeyResult;

public interface CreatePartnerKeyUseCase {
    /** Creates a PARTNER key with custom rate limits. Plain token is returned exactly once. */
    CreatedApiKeyResult createPartnerKey(CreatePartnerKeyCommand command);
}
