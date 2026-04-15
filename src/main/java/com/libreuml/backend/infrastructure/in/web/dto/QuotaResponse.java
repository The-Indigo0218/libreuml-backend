package com.libreuml.backend.infrastructure.in.web.dto;

/**
 * Response body for {@code GET /api/v1/users/me/quota}.
 *
 * @param quota     The user's storage ceiling in bytes (10 485 760 = 10 MB).
 * @param used      Bytes currently consumed across all owned diagrams.
 * @param available Remaining bytes before quota is reached ({@code quota - used}).
 */
public record QuotaResponse(long quota, long used, long available) {}
