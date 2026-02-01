package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.domain.model.User;
import com.libreuml.backend.application.user.port.in.dto.CreateUserCommand;

public interface CreateUserUseCase {

    /**
     * Business logic to create a new user.
     * @param command Data required for creation.
     * @return The created User domain entity.
     */
    User create(CreateUserCommand command);
}