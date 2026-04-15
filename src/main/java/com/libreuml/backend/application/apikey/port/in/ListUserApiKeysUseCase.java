package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.domain.model.ApiKey;

import java.util.List;
import java.util.UUID;

public interface ListUserApiKeysUseCase {
    /** Returns all keys belonging to the given user. Never includes plain-text tokens. */
    List<ApiKey> listByUser(UUID userId);
}
