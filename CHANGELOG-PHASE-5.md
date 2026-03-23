# Phase 5 — Observability: Study Guide & Changelog

## What Was Built

Phase 5 adds production-grade observability to the LibreUML backend across three pillars:

| Pillar | Technology | Entry point |
|---|---|---|
| **Metrics** | Micrometer + Prometheus | `GET /internal/prometheus` |
| **Distributed Tracing** | Micrometer Tracing → OpenTelemetry SDK → OTLP | OTLP collector endpoint |
| **Structured Logging** | Logback + logstash-logback-encoder | stdout (JSON in `prod`, colour in dev) |

All instrumentation strictly respects the Hexagonal Architecture boundary: the application layer
knows only the `MetricsPort` interface, never Micrometer or Prometheus directly.

---

## 1. How Observability Fits Into Hexagonal Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Infrastructure (adapters)                                    │
│  ┌──────────────────────────────────────────┐                │
│  │  MicrometerMetricsAdapter                │ ← implements  │
│  │  (infrastructure.out.metrics)            │                │
│  └──────────────────┬───────────────────────┘                │
│                     │                                         │
│  ┌──────────────────▼───────────────────────────────────┐   │
│  │  Application (use cases)                              │   │
│  │  ┌───────────────────────────────────┐               │   │
│  │  │  MetricsPort                      │ ← port (out)  │   │
│  │  │  (application.common.port.out)    │               │   │
│  │  └───────────────────────────────────┘               │   │
│  │                  ↑  injected into                     │   │
│  │  DiagramService  ·  AuthService  ·  OAuthLoginService │   │
│  │  UserService                                          │   │
│  └───────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### Why the Port Is Essential

Without `MetricsPort`, services would import `io.micrometer.core.instrument.MeterRegistry`
directly. ArchUnit Rule 3 ("Application layer must not depend on infrastructure") would still
technically pass (Micrometer is not in `..infrastructure..`), but a more important boundary
would be broken: **business logic would be coupled to an observability vendor**.

With `MetricsPort`, swapping Micrometer for another registry (Dropwizard Metrics, OpenTelemetry
Metrics SDK) requires changing only `MicrometerMetricsAdapter` — zero changes to any use case.

This is the **Dependency Inversion Principle** applied to observability:
- High-level modules (use cases) depend on the abstraction (`MetricsPort`).
- Low-level modules (Micrometer adapter) implement the abstraction.
- The abstraction belongs to the high-level module's package (`application.common.port.out`).

### ArchUnit Compliance

| Rule | Verification |
|---|---|
| Domain isolation | `MetricsPort` is in `application`, not `domain`. Domain is untouched. |
| Application ↛ Infrastructure | Services inject `MetricsPort`, never `MicrometerMetricsAdapter`. |
| Web ↛ Persistence | No change; metrics wiring is orthogonal to this rule. |
| Persistence ↛ Web | No change. |

---

## 2. Metrics — `MetricsPort` and `MicrometerMetricsAdapter`

### Port (Application Layer)

```java
// application/common/port/out/MetricsPort.java
public interface MetricsPort {
    void incrementDiagramSaved(DiagramType type);   // low-cardinality tag: type
    void incrementUserRegistered();
    void incrementFailedLogin();
    void incrementOAuthLogin(String provider);       // low-cardinality tag: provider
}
```

All methods return `void`. A metric emission must never alter the observable behaviour of a
use case — it is a side-effect that the infrastructure layer handles asynchronously.

### Adapter (Infrastructure Layer)

```java
// infrastructure/out/metrics/MicrometerMetricsAdapter.java
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
    // ...
}
```

`Counter.builder(...).register(meterRegistry)` is idempotent: Micrometer caches meters by
`name + tags` in an O(1) hash map. Repeated calls do not create duplicate meters.

### Prometheus Metric Names

After Prometheus scrapes `/internal/prometheus`, it appends `_total` to counters:

| Method | Prometheus metric |
|---|---|
| `incrementDiagramSaved(SEQUENCE)` | `libreuml_diagrams_saved_total{type="sequence"}` |
| `incrementUserRegistered()` | `libreuml_users_registered_total` |
| `incrementFailedLogin()` | `libreuml_auth_failed_logins_total` |
| `incrementOAuthLogin("github")` | `libreuml_auth_oauth_logins_total{provider="github"}` |

### Where Metrics Are Emitted

| Event | Service method | Counter incremented |
|---|---|---|
| Diagram persisted | `DiagramService.create()` after successful save | `libreuml.diagrams.saved` |
| User registered | `UserService.create()` after successful save | `libreuml.users.registered` |
| Login failed (wrong email) | `AuthService.login()` on `UserNotFoundException` path | `libreuml.auth.failed_logins` |
| Login failed (wrong password) | `AuthService.login()` on `IncorrectPasswordException` path | `libreuml.auth.failed_logins` |
| OAuth login/provision | `OAuthLoginService.login()` after `findOrProvision()` | `libreuml.auth.oauth_logins` |

---

## 3. Actuator Configuration

### Endpoint Exposure

```yaml
management:
  endpoints:
    web:
      base-path: /internal           # all management endpoints under /internal
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized  # full health details require authentication
      probes:
        enabled: true                # /internal/health/liveness + /internal/health/readiness
```

The non-`/api/v1` prefix keeps the management API cleanly separated from the business API
and makes it easy to block `/internal/**` at the load balancer for external traffic.

### Security Rules

```java
// SecurityConfig.java
.requestMatchers("/internal/health/**").permitAll()   // k8s liveness/readiness probes
.requestMatchers("/internal/prometheus").permitAll()  // Prometheus scraper (no auth)
```

`/internal/metrics`, `/internal/info` remain protected by `.anyRequest().authenticated()`.
Prometheus scraping is unauthenticated because the Prometheus server is typically inside
the same private network as the application.  If the scrape endpoint must be public, add
HTTP Basic auth via `management.endpoints.web.exposure.default-role`.

---

## 4. Distributed Tracing

### How Micrometer Tracing Works

```
HTTP Request
     │
     ▼
JwtCookieAuthFilter  (Micrometer creates Span automatically via ServerHttpObservationFilter)
     │
     ▼
Spring MVC  ──── Span: "GET /api/v1/diagrams/{id}"
     │
     ▼
DiagramService  ─── child span (if @Observed or explicit Tracer usage)
     │
     ▼
OtlpHttpSpanExporter ──── exports to OTLP collector (Jaeger, Tempo, etc.)
```

The bridge dependency `micrometer-tracing-bridge-otel` translates Micrometer's `Tracer` API
to the OpenTelemetry SDK. This means all code uses `io.micrometer.tracing.Tracer` (framework-
agnostic), and the underlying wire protocol (OTLP) is an infrastructure concern in
`application.yml`.

### Trace IDs in Logs

Micrometer Tracing populates `traceId` and `spanId` in the MDC (Mapped Diagnostic Context).
Both `logback-spring.xml` profiles surface these values:

- **prod**: `LogstashEncoder` includes all MDC fields in the JSON output automatically.
- **dev**: `[%X{traceId}/%X{spanId}]` appears in the pattern.

### Sampling Configuration

```yaml
# application.yml (prod default: 100 %)
management:
  tracing:
    sampling:
      probability: ${TRACING_PROBABILITY:1.0}

# src/test/resources/application.yml (0 % in tests)
management:
  tracing:
    sampling:
      probability: 0.0
```

`probability: 0.0` in tests suppresses all span creation and prevents any OTLP export
attempts to a non-existent collector, keeping CI output clean and test execution fast.

---

## 5. Structured Logging — `logback-spring.xml`

### Two-profile Design

```xml
<include resource="org/springframework/boot/logging/logback/defaults.xml"/>

<springProfile name="prod">
    <!-- JSON via LogstashEncoder -->
</springProfile>

<springProfile name="!prod">
    <!-- Human-readable via pattern encoder -->
</springProfile>
```

The `<include>` at the top registers Spring Boot's `%clr` (colour) and `%wEx` (whitespace-
aware exception printer) converters globally, before either `<springProfile>` block runs.
These converters are declared in Spring Boot's own `defaults.xml` and would otherwise be
unknown to Logback's pattern parser, causing a fatal startup error.

### Production JSON Format

A typical production log line (pretty-printed for readability):
```json
{
  "@timestamp": "2026-03-22T21:30:15.123Z",
  "@version": "1",
  "message": "Diagram SEQUENCE saved for owner 3f2a…",
  "logger_name": "c.l.b.a.diagram.port.service.DiagramService",
  "level": "INFO",
  "traceId": "8a3f2c1d9e4b7f0a",
  "spanId": "1a2b3c4d",
  "stack_trace": null
}
```

### Development Pattern

```
2026-03-22 21:30:15.123  INFO 12345 --- [8a3f2c1d9e4b7f0a/1a2b3c4d] c.l.b.a.d.p.s.DiagramService : Diagram SEQUENCE saved for owner 3f2a…
```

---

## 6. Dependencies Added to `pom.xml`

All versions are managed by the Spring Boot 3.5.x BOM unless noted.

| Artifact | Purpose |
|---|---|
| `spring-boot-starter-actuator` | Health, info, metrics, and prometheus endpoints |
| `io.micrometer:micrometer-registry-prometheus` | Exposes Micrometer meters in Prometheus text format |
| `io.micrometer:micrometer-tracing-bridge-otel` | Bridges Micrometer Tracing API → OpenTelemetry SDK |
| `io.opentelemetry:opentelemetry-exporter-otlp` | Exports spans via OTLP to Jaeger / Grafana Tempo |
| `net.logstash.logback:logstash-logback-encoder:8.0` | JSON log encoding compatible with Logback 1.5.x (Spring Boot 3.3+) |

`logstash-logback-encoder` is the only dependency with an explicit version because it is
not part of the Spring Boot BOM.  Version 8.0 targets Logback 1.5.x, which Spring Boot 3.3+
ships with.

---

## 7. Files Created / Modified

### New Files

| File | Purpose |
|---|---|
| `application/common/port/out/MetricsPort.java` | Outbound port — defines business metric operations without coupling to any framework |
| `infrastructure/out/metrics/MicrometerMetricsAdapter.java` | Implements `MetricsPort` using Micrometer `Counter`s |
| `src/main/resources/logback-spring.xml` | Profile-aware logging: JSON (prod) vs. human-readable (dev) |

### Modified Files

| File | Change |
|---|---|
| `pom.xml` | Added 5 observability dependencies |
| `src/main/resources/application.yml` | Added `management.*` block (actuator, tracing, OTLP) |
| `src/test/resources/application.yml` | Set `management.tracing.sampling.probability: 0.0` to suppress tracing in tests |
| `SecurityConfig.java` | Added `permitAll()` for `/internal/health/**` and `/internal/prometheus` |
| `DiagramService.java` | Injected `MetricsPort`; calls `incrementDiagramSaved()` after successful create |
| `AuthService.java` | Injected `MetricsPort`; calls `incrementFailedLogin()` on both failure paths |
| `OAuthLoginService.java` | Injected `MetricsPort`; calls `incrementOAuthLogin()` after identity resolution |
| `UserService.java` | Injected `MetricsPort`; calls `incrementUserRegistered()` after successful registration |
| `UserServiceTest.java` | Added `@Mock MetricsPort metricsPort` so Mockito's `@InjectMocks` can satisfy the new constructor parameter |

---

## 8. Alerting Recipes (Reference)

These PromQL expressions illustrate the practical value of the counters just added:

```promql
# Failed login rate over 5 minutes (alert if > 10/min)
rate(libreuml_auth_failed_logins_total[5m]) * 60 > 10

# Diagram creation throughput by type
sum by (type) (rate(libreuml_diagrams_saved_total[1m]))

# Registration funnel: standard vs. OAuth
sum(rate(libreuml_users_registered_total[1h]))
  /
(sum(rate(libreuml_users_registered_total[1h])) + sum(rate(libreuml_auth_oauth_logins_total[1h])))
```
