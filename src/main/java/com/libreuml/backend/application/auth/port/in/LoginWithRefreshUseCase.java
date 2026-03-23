package com.libreuml.backend.application.auth.port.in;

import com.libreuml.backend.application.auth.dto.TokenPair;
import com.libreuml.backend.application.user.port.in.dto.LoginCommand;

public interface LoginWithRefreshUseCase {
    TokenPair login(LoginCommand command, String ipAddress, String userAgent);
}
