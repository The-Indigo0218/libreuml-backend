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
}
