package com.libreuml.backend.infrastructure.out.metrics;

import com.libreuml.backend.application.common.port.out.MetricsPort;
import com.libreuml.backend.domain.model.DiagramType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
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

    @Override
    public void incrementActiveUsersDaily(String method) {
        Counter.builder("libreuml.active.users.daily")
                .tag("method", method)
                .description("Successful logins per authentication method — proxy for daily active users")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void incrementQuotaRejection() {
        Counter.builder("libreuml.quota.rejection")
                .description("Total requests rejected with HTTP 422 due to storage quota exhaustion")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void observeUserStorageBytes(long bytes) {
        // High-cardinality user_id labels are avoided by recording the distribution instead.
        // Bucket boundaries follow the 5 MB per-user quota ceiling (5_242_880 bytes).
        DistributionSummary.builder("libreuml.storage.used.bytes")
                .description("Distribution of storage_used_bytes across users, sampled on every storage mutation")
                .baseUnit("bytes")
                .serviceLevelObjectives(
                        102_400,   // 100 KB
                        524_288,   // 512 KB
                        1_048_576, // 1 MB
                        2_097_152, // 2 MB
                        3_145_728, // 3 MB
                        4_194_304, // 4 MB
                        5_242_880  // 5 MB (quota ceiling)
                )
                .register(meterRegistry)
                .record(bytes);
    }
}
