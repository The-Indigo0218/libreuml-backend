package com.libreuml.backend.application.apikey.port.in;

import java.util.UUID;

public interface RevokeApiKeyUseCase {
    /** Revokes a USER key. Throws {@code ApiKeyOwnershipException} if the key does not belong to {@code requesterId}. */
    void revoke(UUID keyId, UUID requesterId);
}
