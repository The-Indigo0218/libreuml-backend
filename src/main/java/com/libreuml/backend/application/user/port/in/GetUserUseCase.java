package com.libreuml.backend.application.user.port.in;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.User;


import java.util.Optional;
import java.util.UUID;

public interface GetUserUseCase {

    User getUserById(UUID id);

    Optional<User> getUserByEmail(String email);

    PagedResult<User> getAllUsers(PaginationCommand command);

    PagedResult<User> getUsersByFullName(String fullName, PaginationCommand command);

    PagedResult<User> getActiveUsers(PaginationCommand command);

    int getActiveUsersCount();

    int getTotalUsersCount();

}
