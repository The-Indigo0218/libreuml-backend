package com.libreuml.backend.infrastructure.out.metrics;

import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.domain.model.DiagramType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of {@link MetricsPort}.
 *
 * <p>All counters use the {@code libreuml.*} prefix and follow Prometheus naming conventions
 * (snake_case, {@code _total} suffix appended automatically by the Prometheus registry).
 * Low-cardinality tags are attached where they add analytical value (e.g., {@code type}
 * on diagram metrics, {@code provider} on OAuth metrics).
 *
 * <p>Each {@link Counter} is resolved through the registry on every call.  Micrometer
 * caches meters by name+tags internally, so repeated lookups are O(1) hash-map reads —
 * there is no need to pre-store counter references as fields.
 *
 * <p>This class resides in {@code infrastructure.out.metrics} — the correct side of the
 * hexagonal boundary.  Services in the application layer depend only on {@link MetricsPort},
 * never on this concrete class.
 */
@Component
@RequiredArgsConstructor
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry meterRegistry;

    @Override
    public void incrementDiagramSaved(DiagramType type) {
        Counter.builder("libreuml.diagrams.saved")
                .tag("type", type.name().toLowerCase())
                .description("Total number of diagrams successfully persisted")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void incrementUserRegistered() {
        Counter.builder("libreuml.users.registered")
                .description("Total number of users registered via the standard sign-up flow")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void incrementFailedLogin() {
        Counter.builder("libreuml.auth.failed_logins")
                .description("Total number of failed authentication attempts (wrong credentials or unknown user)")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void incrementOAuthLogin(String provider) {
        Counter.builder("libreuml.auth.oauth_logins")
                .tag("provider", provider)
                .description("Total number of successful OAuth logins or auto-provisioning events")
                .register(meterRegistry)
                .increment();
    }
}
