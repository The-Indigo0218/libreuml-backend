package com.libreuml.backend.application.user.port.in;

import java.util.UUID;

public interface DeleteAccountUseCase {
    void deleteAccount(UUID userId);
}
