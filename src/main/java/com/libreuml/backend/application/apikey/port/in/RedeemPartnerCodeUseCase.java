package com.libreuml.backend.application.apikey.port.in;

import com.libreuml.backend.application.apikey.dto.CreatedApiKeyResult;

import java.util.UUID;

public interface RedeemPartnerCodeUseCase {
    /**
     * Redeems a partner redemption code for the given user.
     *
     * <p>Creates a new USER key with the partner key's rate limits and scope,
     * increments the partner key's redemption counter, and returns the new
     * key's plain token exactly once.
     */
    CreatedApiKeyResult redeem(String redemptionCode, UUID userId);
}
