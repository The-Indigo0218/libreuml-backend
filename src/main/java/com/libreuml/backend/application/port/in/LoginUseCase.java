package com.libreuml.backend.application.port.in;

import com.libreuml.backend.application.port.in.dto.LoginCommand;

public interface LoginUseCase {
    String login(LoginCommand command);
}