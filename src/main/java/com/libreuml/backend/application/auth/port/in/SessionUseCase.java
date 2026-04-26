package com.libreuml.backend.application.auth.port.in;

import com.libreuml.backend.domain.model.RefreshToken;

import java.util.List;
import java.util.UUID;

public interface SessionUseCase {
    List<RefreshToken> listSessions(UUID userId);
    void revokeSession(UUID tokenId, UUID userId);
}
