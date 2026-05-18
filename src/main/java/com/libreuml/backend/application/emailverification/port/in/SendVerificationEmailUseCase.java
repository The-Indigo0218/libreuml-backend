package com.libreuml.backend.application.emailverification.port.in;

import java.util.UUID;

public interface SendVerificationEmailUseCase {
    void send(UUID userId);
}
