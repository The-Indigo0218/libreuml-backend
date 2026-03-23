package com.libreuml.backend.application.auth.port.in;

import com.libreuml.backend.application.auth.dto.RefreshCommand;
import com.libreuml.backend.application.auth.dto.TokenPair;

public interface RefreshTokenUseCase {
    TokenPair refresh(RefreshCommand command);
    void revoke(String rawRefreshToken);
}
