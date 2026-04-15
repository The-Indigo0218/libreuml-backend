package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.domain.model.ApiKey;

import java.util.UUID;

public interface GetPartnerKeyUseCase {
    ApiKey getPartnerKey(UUID id);
}
