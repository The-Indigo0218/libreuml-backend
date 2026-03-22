# 01 — V1 Gap Analysis

**Audit Date:** 2026-03-21
**Codebase:** LibreUML Backend (Spring Boot 3.5.10 / Java 21)
**Auditor Role:** Lead Security & Software Architect

---

## Executive Summary

The current backend covers the LibreUML Academy foundation (users, courses, resources, enrollments, reports, Q&A) but is **missing 4 of the 5 core v1 pillars**. Roughly 35–40% of the product surface required for a stable v1 public release remains unbuilt. The gaps are not cosmetic — they affect security posture (JWT transport), monetization telemetry, the primary social login flow, and the signature feature of the product (diagram sync).

---

## Gap 1 — GitHub OAuth Flow

**Status: 0% implemented**

### What is missing

| Missing Piece | Description |
|---|---|
| `spring-boot-starter-oauth2-client` | Not present in `pom.xml`. The entire OAuth2 client machinery is absent. |
| `OAuthUserInfo` domain model | No value object to represent the GitHub profile payload (`login`, `avatar_url`, `email`, `id`). |
| `GitHubOAuthAdapter` | No outbound port adapter to exchange the authorization code for an access token and then call `api.github.com/user`. |
| `OAuthCallbackController` | No `GET /api/v1/auth/oauth2/callback/github` endpoint to receive the authorization code from GitHub. |
| `ProvideOAuthLoginUseCase` | No application use case to: (1) look up existing user by `github_id`, (2) auto-register if first-time, (3) issue a JWT or set an HttpOnly cookie. |
| DB column `github_id` | The `users` table has `github_url` (display), but no `github_id` (the stable numeric identifier from GitHub used for de-duplication). |
| DB column `oauth_provider` | No column to distinguish local accounts from OAuth-created accounts. |
| `state` parameter CSRF protection | OAuth2 flow requires a `state` parameter signed with a session-scoped secret to prevent CSRF on the callback. No mechanism exists. |
| PKCE support | For public/SPA clients the OAuth2 Authorization Code + PKCE extension must be used. No PKCE verifier generation or validation code exists. |

### Architectural location for the fix

```
application/
  auth/
    port/in/
      OAuthLoginUseCase.java          ← new
    port/out/
      OAuthProviderPort.java          ← new (fetch user profile from provider)
    service/
      OAuthLoginService.java          ← new
    dto/
      OAuthUserInfo.java              ← new value object

infrastructure/
  out/oauth/
    GitHubOAuthAdapter.java           ← implements OAuthProviderPort
  in/web/controller/
    OAuthCallbackController.java      ← new
```

### DB migration required

```sql
-- V6__add_oauth_support.sql
ALTER TABLE users ADD COLUMN github_id VARCHAR(100) UNIQUE;
ALTER TABLE users ADD COLUMN oauth_provider VARCHAR(50);
ALTER TABLE users ADD COLUMN local_password_set BOOLEAN DEFAULT TRUE;
CREATE INDEX idx_users_github_id ON users(github_id);
```

---

## Gap 2 — JWT as HttpOnly Cookie (Secure Transport)

**Status: 0% implemented — JWT is in a bearer header only**

### What is missing

The current flow returns the JWT in a JSON response body:

```java
// AuthController.java (current)
return ResponseEntity.ok(new AuthResponse(token));
```

The frontend must store this in `localStorage` or `sessionStorage`, both of which are **XSS-readable**. The v1 requirement is to set the JWT inside an `HttpOnly; Secure; SameSite=Strict` cookie so JavaScript can never access it.

| Missing Piece | Description |
|---|---|
| `CookieTokenStrategy` | No component that sets / reads / clears a cookie named `__Host-jwt` (or similar). |
| Logout endpoint | `DELETE /api/v1/auth/logout` which clears the cookie server-side. Does not exist. |
| `JwtCookieFilter` | The current `JwtAuthenticationFilter` only reads `Authorization: Bearer`. It does not look in cookies. |
| Refresh token model | Short-lived access token (15 min) + long-lived refresh token (7 days) in a separate `__Host-refresh` cookie. No such model exists. |
| `refresh_tokens` table | No DB table to store revocable refresh tokens. |
| CSRF token endpoint | When using cookies, a CSRF token must be issued (e.g. via `/api/v1/auth/csrf`) and validated on state-mutating requests. |

### Architectural location for the fix

```
infrastructure/security/
  cookie/
    CookieTokenStrategy.java          ← new (set/read/clear HttpOnly cookies)
    JwtCookieAuthFilter.java          ← new (reads JWT from cookie, not header)
  config/
    SecurityConfig.java               ← update: add CsrfTokenRepository for cookie auth path

application/auth/port/out/
  RefreshTokenRepository.java         ← new output port

infrastructure/out/persistence/
  entity/
    RefreshTokenEntity.java           ← new JPA entity
  adapter/
    RefreshTokenPersistenceAdapter.java  ← new
```

---

## Gap 3 — Telemetry / Metrics Ingestion

**Status: ~2% implemented (only SLF4J logging exists)**

### What is missing

| Missing Piece | Description |
|---|---|
| `spring-boot-starter-actuator` | Not in `pom.xml`. No `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` endpoints. |
| `micrometer-registry-prometheus` | Not in `pom.xml`. No Prometheus scrape endpoint. |
| Custom business metrics | No counters for `user.registrations`, `diagram.saves`, `login.failures`, `oauth.logins`. |
| Distributed tracing | No `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`. No trace IDs on requests. |
| Structured logging | Plain text logs. No JSON log format (e.g. `logstash-logback-encoder`) for log aggregation pipelines. |
| Security event audit log | No dedicated audit trail for login, password change, role escalation, and admin actions. |
| `MetricsPort` outbound port | No application-layer port to record domain events as metrics (follows Hexagonal principle). |

### What the `application.yml` is missing

```yaml
# Missing production observability config
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /internal   # NOT exposed on the public API path
  endpoint:
    health:
      show-details: never    # Never expose internal details publicly
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

### Custom metric example (what should be built)

```java
// application/common/port/out/MetricsPort.java
public interface MetricsPort {
    void incrementCounter(String name, String... tags);
    void recordGauge(String name, double value, String... tags);
    void recordTimer(String name, Duration duration, String... tags);
}

// infrastructure/out/metrics/MicrometerMetricsAdapter.java
@Component
public class MicrometerMetricsAdapter implements MetricsPort {
    private final MeterRegistry registry;
    // ...
}
```

---

## Gap 4 — Diagram JSONB Cloud Sync

**Status: 0% implemented — the core product feature is entirely absent from the backend**

### What is missing

This is the most critical gap. LibreUML's primary value proposition is a UML editor. There is **no domain model, no API endpoint, no database schema, and no service logic** for diagram persistence.

| Missing Piece | Description |
|---|---|
| `Diagram` aggregate root | No domain object for a UML diagram (`id`, `ownerId`, `title`, `diagramType`, `content` as JSONB, `createdAt`, `updatedAt`, `version`). |
| `DiagramVersion` entity | No version history / snapshot model for conflict resolution. |
| `SaveDiagramUseCase` | No use case to create or update a diagram's JSONB content. |
| `GetDiagramUseCase` | No use case to retrieve a diagram by ID with ownership enforcement. |
| `ShareDiagramUseCase` | No sharing model (public link, collaborator invite). |
| `DiagramController` | No REST endpoints (`POST /api/v1/diagrams`, `PUT /api/v1/diagrams/{id}`, `GET /api/v1/diagrams/{id}`, etc.). |
| DB migration V6 | No `diagrams` table with a `content JSONB NOT NULL` column. |
| `DiagramType` enum | No enum covering `CLASS`, `SEQUENCE`, `USECASE`, `ACTIVITY`, `COMPONENT`, `STATE`, `ER`. |
| Optimistic locking | No `@Version` field on the entity to prevent lost-update conflicts in concurrent saves. |
| Local-first sync protocol | No conflict resolution strategy (e.g. last-write-wins with `updated_at`, or CRDTs) for offline-first clients reconnecting. |
| Export port | No `ExportDiagramPort` for producing XMI / SVG / PNG from server side. |

### DB migration required

```sql
-- V7__create_diagrams.sql
CREATE TABLE diagrams (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    diagram_type   VARCHAR(50)  NOT NULL,
    content        JSONB        NOT NULL DEFAULT '{}',
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_diagrams_owner   ON diagrams(owner_id);
CREATE INDEX idx_diagrams_type    ON diagrams(diagram_type);
CREATE INDEX idx_diagrams_content ON diagrams USING gin(content);  -- JSONB search

CREATE TABLE diagram_collaborators (
    diagram_id   UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission   VARCHAR(20) NOT NULL DEFAULT 'VIEW',  -- VIEW | EDIT
    added_at     TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (diagram_id, user_id)
);
```

### Minimal domain model required

```java
// domain/model/Diagram.java
@Builder
public class Diagram {
    private UUID   id;
    private UUID   ownerId;
    private String title;
    private DiagramType diagramType;
    private JsonNode    content;       // Jackson JsonNode for JSONB
    private boolean     isPublic;
    private long        version;       // optimistic lock
    private Instant     createdAt;
    private Instant     updatedAt;

    public void updateContent(UUID requesterId, JsonNode newContent) {
        if (!this.ownerId.equals(requesterId)) {
            throw new UserNotAuthorizedException("Only the owner can modify this diagram.");
        }
        this.content   = newContent;
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void makePublic(UUID requesterId)  { /* authorization */ }
    public void makePrivate(UUID requesterId) { /* authorization */ }
}
```

---

## Gap 5 — Missing Question/Answer Persistence Layer

**Status: ~40% implemented — domain and service layer exist, persistence is broken**

The `QuestionRepositoryAdapter` and `AnswerRepositoryAdapter` are present in the codebase but reference Spring Data JPA repositories (`SpringDataQuestionRepository`, `SpringDataAnswerRepository`) that **do not have corresponding JPA entity classes or database migrations**.

| Missing Piece | Description |
|---|---|
| `QuestionEntity.java` | No JPA entity. The adapter presumably fails at startup or uses an incomplete stub. |
| `AnswerEntity.java` | Same. |
| `V6__create_qa_tables.sql` (or similar) | No migration creates `questions` or `answers` tables. |
| `QuestionPersistenceMapper.java` | No mapper between `Question` domain and `QuestionEntity` JPA (the Question mapper exists in `application/question/port/mapper/` but that maps domain ↔ DTO, not domain ↔ entity). |
| `AnswerPersistenceMapper.java` | Same. |

This is a **silent runtime failure**: the app may compile and start, but any question/answer operation will throw a `BeanCreationException` or a Hibernate `UnknownEntityTypeException`.

---

## Summary Table

| V1 Pillar | Completion | Blocking Risk |
|---|---|---|
| Academy (courses, resources, enrollments, reports) | ~85% | Low |
| Q&A (questions, answers) | ~50% | Medium (persistence layer broken) |
| GitHub OAuth | 0% | High |
| JWT HttpOnly Cookie | 0% | High (XSS exposure) |
| Telemetry / Metrics | 2% | Medium |
| Diagram Cloud Sync | 0% | Critical (core product feature) |
