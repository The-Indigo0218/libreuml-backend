package com.libreuml.backend.application.passwordreset.port.in;

public interface RequestPasswordResetUseCase {
    void request(String email);
}
