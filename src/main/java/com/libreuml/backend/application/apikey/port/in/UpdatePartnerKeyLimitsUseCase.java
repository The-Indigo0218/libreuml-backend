package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.apikey.dto.UpdatePartnerKeyLimitsCommand;
import com.libreuml.backend.domain.model.ApiKey;

public interface UpdatePartnerKeyLimitsUseCase {
    ApiKey updateLimits(UpdatePartnerKeyLimitsCommand command);
}
