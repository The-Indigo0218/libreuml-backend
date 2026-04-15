package com.libreuml.backend.application.apikey.port.in;

import java.util.UUID;

public interface RevokePartnerKeyUseCase {
    void revokePartnerKey(UUID keyId, UUID adminId);
}
