package com.libreuml.backend.application.user.port.out;

import com.libreuml.backend.application.common.PagedResult;
import com.libreuml.backend.application.common.dto.PaginationCommand;
import com.libreuml.backend.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    boolean existsByEmail(String email);

    Optional<User> getUserById(UUID id);

    Optional<User> findByEmail(String email);

    PagedResult<User> getUserByFullName(String fullName, PaginationCommand command);

    PagedResult<User> getAllUsers(PaginationCommand command);

    PagedResult<User> getActiveUsers(PaginationCommand command);

    int getActiveUsersCount();

    int getTotalUsersCount();


}