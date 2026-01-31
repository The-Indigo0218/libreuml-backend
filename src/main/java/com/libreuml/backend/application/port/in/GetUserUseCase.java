package com.libreuml.backend.application.port.in;

import com.libreuml.backend.application.port.in.dto.GetUserByIdCommand;
import com.libreuml.backend.domain.model.User;

public interface GetUserUseCase {

    /**
     * business logic to find a user
     * @param id to search user
     * @return the find user
     */
    User getUserById(GetUserByIdCommand id);
}
