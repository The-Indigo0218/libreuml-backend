package com.libreuml.backend.application.emailverification.port.in;

public interface ConfirmEmailUseCase {
    void confirm(String rawToken);
}
