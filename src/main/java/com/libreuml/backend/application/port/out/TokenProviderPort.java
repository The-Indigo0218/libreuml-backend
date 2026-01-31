package com.libreuml.backend.application.port.out;

import com.libreuml.backend.domain.model.User;

public interface TokenProviderPort {
    String generateToken(User user);
}