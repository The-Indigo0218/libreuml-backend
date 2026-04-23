package com.libreuml.backend.infrastructure.in.web.dto;

/**
 * Response body for {@code GET /api/v1/users/me/quota}.
 *
 * @param quota     The user's storage ceiling in bytes (5 242 880 = 5 MB).
 * @param used      Bytes currently consumed across all owned projects (models + diagrams).
 * @param available Remaining bytes before quota is reached ({@code quota - used}).
 * @param breakdown Per-resource usage breakdown.
 */
public record QuotaResponse(long quota, long used, long available, Breakdown breakdown) {
    public record Breakdown(long models, long diagrams) {}
}
