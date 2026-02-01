package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.application.user.port.in.dto.LoginCommand;

public interface LoginUseCase {
    String login(LoginCommand command);
}