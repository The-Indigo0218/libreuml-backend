package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface GetUserUseCase {

    User getUserById(UUID id);

    List<User> getUserByFullName(String fullName);

    User getUserByEmail(String email);
}
