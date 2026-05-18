package com.libreuml.backend.application.common.port.out;

import com.libreuml.backend.domain.model.DiagramType;

/**
 * Output port for recording business-level metrics.
 *
 * <p>This interface lives in the application layer so that use-case services can record
 * meaningful business events without importing anything from Micrometer, Prometheus, or
 * any other observability framework.  The concrete implementation ({@code MicrometerMetricsAdapter})
 * lives in the infrastructure layer, keeping the hexagonal boundary intact.
 *
 * <p>All methods are deliberately fire-and-forget (void return).  A metric emission must
 * never alter the observable behaviour of a use case — it is a side-effect that the
 * infrastructure layer handles.
 */
public interface MetricsPort {

    /**
     * Records that a diagram was successfully persisted.
     *
     * @param type the diagram type used as a low-cardinality Prometheus label
     */
    void incrementDiagramSaved(DiagramType type);

    /**
     * Records that a new user account was created via the standard registration flow.
     * OAuth-provisioned accounts are tracked separately by the {@link #incrementOAuthLogin} metric.
     */
    void incrementUserRegistered();

    /**
     * Records that an authentication attempt failed due to unknown email or wrong password.
     * Useful for alerting on brute-force patterns.
     */
    void incrementFailedLogin();

    /**
     * Records that a user logged in (or was auto-provisioned) via an OAuth provider.
     *
     * @param provider lowercase provider name (e.g., {@code "github"}, {@code "google"})
     */
    void incrementOAuthLogin(String provider);

    /**
     * Increments the daily active-user counter.
     *
     * @param method {@code "credential"} for password login, {@code "github"} or {@code "google"} for OAuth
     */
    void incrementActiveUsersDaily(String method);

    /**
     * Increments the quota-rejection counter whenever a request is rejected with HTTP 422
     * due to a storage quota breach.
     */
    void incrementQuotaRejection();

    /**
     * Records the current {@code storage_used_bytes} of a single user as a histogram observation.
     * Used to track the distribution of per-user storage without high-cardinality {@code user_id} labels.
     *
     * @param bytes current storage used by the user, in bytes
     */
    void observeUserStorageBytes(long bytes);
}
