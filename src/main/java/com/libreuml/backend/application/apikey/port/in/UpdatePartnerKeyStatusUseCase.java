package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.apikey.dto.UpdatePartnerKeyStatusCommand;
import com.libreuml.backend.domain.model.ApiKey;

public interface UpdatePartnerKeyStatusUseCase {
    ApiKey updateStatus(UpdatePartnerKeyStatusCommand command);
}
