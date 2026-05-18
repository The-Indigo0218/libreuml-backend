package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.domain.model.ApiKey;

public interface ListPartnerKeysUseCase {
    PagedResult<ApiKey> listPartnerKeys(int page, int size);
}
