package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.application.auth.dto.OAuthProvider;
import com.libreuml.backend.domain.model.User;

import java.util.UUID;

public interface UnlinkOAuthUseCase {
    User unlinkOAuth(UUID userId, OAuthProvider provider);
}
