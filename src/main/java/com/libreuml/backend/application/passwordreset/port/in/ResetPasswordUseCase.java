package com.libreuml.backend.application.passwordreset.port.in;

public interface ResetPasswordUseCase {
    void reset(String rawToken, String newPassword);
}
