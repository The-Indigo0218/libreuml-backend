package com.libreuml.backend.application.auth.port.in;

import com.libreuml.backend.application.auth.dto.OAuthCallbackCommand;
import com.libreuml.backend.application.auth.dto.TokenPair;

public interface OAuthLoginUseCase {
    TokenPair login(OAuthCallbackCommand command);
}
