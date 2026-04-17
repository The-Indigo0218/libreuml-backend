package com.libreuml.backend.infrastructure.config;

import com.libreuml.backend.application.user.port.out.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    /**
     * Registers a Gauge that reports the live SUM(storage_used_bytes) across all users.
     * Micrometer pulls the value on every Prometheus scrape, so the gauge always reflects
     * the current DB state — including across restarts and without delta tracking in services.
     *
     * <p>The query is a lightweight aggregate (no full-table scan required when
     * the column is summed at the storage engine level).
     */
    @Bean
    public Gauge globalStorageGauge(MeterRegistry registry, UserRepository userRepository) {
        return Gauge.builder("libreuml.storage.used.bytes.total", userRepository, UserRepository::sumStorageUsedBytes)
                .description("Current total storage used across all users (live DB aggregate)")
                .baseUnit("bytes")
                .register(registry);
    }
}
