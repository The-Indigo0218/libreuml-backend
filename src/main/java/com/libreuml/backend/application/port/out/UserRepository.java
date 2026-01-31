package com.libreuml.backend.application.port.out;

import com.libreuml.backend.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    /**
     * Persists a User entity into the database.
     * @param user The domain entity to save.
     * @return The saved user (with ID generated).
     */
    User save(User user);

    /**
     * Checks if a user already exists with the given email.
     * @param email The email to verify.
     * @return true if exists, false otherwise.
     */
    boolean existsByEmail(String email);

    /**
     * gets a user if exist id
     * @param  id to find the user
     * @return user if exist id, exception otherwise
     */
    Optional<User> getUserById(UUID id);

    Optional<User> findByEmail(String email);
}