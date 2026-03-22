# 06 — Execution Roadmap

**Audit Date:** 2026-03-21
**Goal:** Complete the remaining ~35% of the backend to a production-ready, public-safe state.
**Constraint:** Zero critical/high vulnerabilities before the repository goes public.

---

## How to Read This Document

Each phase has:
- **Objective:** What changes when this phase is done.
- **Tasks:** Specific, actionable implementation items.
- **Security Gate:** The checklist that must pass before moving to the next phase. No exceptions.
- **Definition of Done:** Verifiable completion criteria.

Phases are sequential. Do not start Phase 2 before Phase 1's Security Gate passes.

---

## Phase 0 — Security Hardening (Pre-Condition for Going Public)

**Objective:** Eliminate all vulnerabilities that exist in the *currently written* code before adding any new feature. This phase is not optional. The repository must not go public until every item here is complete.

### Tasks

**P0-T1: Remove the JWT hardcoded fallback secret**
- File: `infrastructure/security/JwtAdapter.java`
- Remove the fallback value from `@Value("${jwt.secret:...default...}")`.
- Change to `@Value("${jwt.secret}")` — no default.
- Add a `@PostConstruct` validator that checks the decoded secret is ≥ 64 bytes and throws `IllegalStateException` with a helpful message if not.
- Update `.env.example` with `JWT_SECRET=<generate with: openssl rand -base64 64>`.
- Update `README.md` with required environment variables.

**P0-T2: Disable Spring Security DEBUG logs in base config**
- File: `src/main/resources/application.yml`
- Remove `org.springframework.security: DEBUG` from base config.
- Move to `src/main/resources/application-dev.yml`.

**P0-T3: Disable `show-sql` and `format_sql` in base config**
- File: `src/main/resources/application.yml`
- Set `show-sql: false` in base config.
- Move `show-sql: true` and `format_sql: true` to `application-dev.yml`.

**P0-T4: Add `@PathVariable UUID` type enforcement to all controllers**
- Files: all controllers in `infrastructure/in/web/controller/`.
- Replace all `@PathVariable String id` with `@PathVariable UUID id`.
- Add `MethodArgumentTypeMismatchException` handler in `GlobalControllerAdvice`.

**P0-T5: Add ADMIN password minimum length enforcement**
- File: `infrastructure/out/persistence/seeder/AdminSeeder.java`
- Add `@PostConstruct` check: if `ADMIN_PASSWORD` length < 20 characters, throw `IllegalStateException`.

**P0-T6: Upgrade BCrypt to Argon2id**
- File: `infrastructure/security/config/PasswordEncoderConfig.java`
- Replace `new BCryptPasswordEncoder()` with `new Argon2PasswordEncoder(19456, 2, 1, 32, 64)`.
- Existing BCrypt passwords remain valid (Spring Security's `DelegatingPasswordEncoder` handles mixed formats automatically). Use `DelegatingPasswordEncoder` with Argon2id as default and BCrypt as a legacy fallback.

**P0-T7: Add security response headers**
- File: `infrastructure/security/config/SecurityConfig.java`
- Add `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`, `Cache-Control: no-store`.

**P0-T8: Make CORS configuration environment-driven**
- File: `infrastructure/security/config/SecurityConfig.java`
- Read allowed origins from `${app.cors.allowed-origins}` (a list).
- Add to `application.yml` (dev defaults: localhost:3000, localhost:5173).
- Document the production override requirement.

**P0-T9: Fix `EnrollmentService` package location**
- Move from `application/enrollment/service/` to `application/enrollment/port/service/`.

**P0-T10: Add ArchUnit rules for critical architecture invariants**
- Add the four ArchUnit rules documented in `03-hexagonal-ddd-architecture.md`.
- Run `./mvnw test` — all ArchUnit tests must pass before continuing.

### Security Gate 0 (must pass 100%)

- [ ] `grep -rn "default_super_secure"` returns zero results in the entire project.
- [ ] `grep -rn "jakarta.persistence" src/main/java/com/libreuml/backend/domain/` returns zero results.
- [ ] `./mvnw test` — ArchUnit tests all pass.
- [ ] Manual test: starting the app with `JWT_SECRET` unset throws a clear `IllegalStateException` at startup.
- [ ] Manual test: starting the app with `ADMIN_PASSWORD` shorter than 20 chars throws a clear `IllegalStateException`.
- [ ] Code review: no `@PathVariable String` for ID parameters in any controller.

---

## Phase 1 — Fix Broken Features (Q&A Persistence)

**Objective:** The Q&A system is declared done but is silently broken at the persistence layer. This phase makes it actually work.

### Tasks

**P1-T1: Create Flyway migration for Q&A tables**
- Create `src/main/resources/db/migration/V6__create_qa_tables.sql`.
- Tables: `questions`, `answers` (full schema in `01-v1-gap-analysis.md`).
- GIN index on `questions.tags` for JSONB search.

**P1-T2: Create `QuestionEntity.java`**
- File: `infrastructure/out/persistence/entity/QuestionEntity.java`
- Annotations: `@Entity @Table(name = "questions")`.
- All fields mapped to the V6 migration columns.
- `@ManyToOne(fetch = FetchType.LAZY)` to `UserEntity` for creator.

**P1-T3: Create `AnswerEntity.java`**
- File: `infrastructure/out/persistence/entity/AnswerEntity.java`
- `@ManyToOne(fetch = FetchType.LAZY)` to `QuestionEntity` and `UserEntity`.

**P1-T4: Create `SpringDataQuestionRepository.java`**
- `JpaRepository<QuestionEntity, UUID>` with:
  - `findAllByActiveTrue(Pageable)`
  - `findByCreatorId(UUID, Pageable)`
  - `findByTitleContainingIgnoreCaseAndActiveTrue(String, Pageable)`

**P1-T5: Create `SpringDataAnswerRepository.java`**
- `JpaRepository<AnswerEntity, UUID>` with:
  - `findAllByQuestionIdAndActiveTrue(UUID)`
  - `findAllByCreatorId(UUID)`

**P1-T6: Create `QuestionPersistenceMapper.java`**
- File: `infrastructure/out/persistence/mapper/QuestionPersistenceMapper.java`
- MapStruct mapper: `Question` domain ↔ `QuestionEntity`.

**P1-T7: Create `AnswerPersistenceMapper.java`**
- File: `infrastructure/out/persistence/mapper/AnswerPersistenceMapper.java`

**P1-T8: Update adapters to use new entities and mappers**
- Update `QuestionRepositoryAdapter.java` and `AnswerRepositoryAdapter.java`.

**P1-T9: Add Q&A controllers**
- `QuestionController.java` — CRUD for questions.
- `AnswerController.java` — CRUD for answers.
- Wire to existing use cases.

**P1-T10: Write integration tests for Q&A**
- `QuestionServiceTest.java` — verify create, deactivate, resolve work against a real Testcontainers DB.
- `AnswerServiceTest.java` — verify create, accept, deactivate.
- Security tests: verify only owner/MODERATOR/ADMIN can deactivate.

### Security Gate 1

- [ ] `./mvnw test` — all Q&A integration tests pass against a real PostgreSQL container.
- [ ] `GET /api/v1/questions` returns 200 with correct pagination.
- [ ] `DELETE /api/v1/questions/{id}` by non-owner returns 403.
- [ ] `POST /api/v1/answers` by unauthenticated user returns 401.

---

## Phase 2 — JWT Cookie Transport + Rate Limiting

**Objective:** Eliminate the XSS token theft attack surface and brute-force attack surface before the API is exposed publicly.

### Tasks

**P2-T1: Implement `CookieTokenStrategy`**
- File: `infrastructure/security/cookie/CookieTokenStrategy.java`
- Methods: `setAccessTokenCookie(response, token)`, `clearTokenCookies(response)`, `extractTokenFromCookie(request)`.
- Cookie: `__Host-jwt`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api`, `MaxAge=900` (15 min).

**P2-T2: Implement `JwtCookieAuthFilter`**
- File: `infrastructure/security/JwtCookieAuthFilter.java`
- Replaces or extends `JwtAuthenticationFilter`.
- Reads token from `__Host-jwt` cookie (falls back to `Authorization: Bearer` header for API clients and mobile).

**P2-T3: Update `AuthController`**
- `POST /api/v1/auth/login` → set cookie via `CookieTokenStrategy`, return `204 No Content` (no token in body).
- Add `DELETE /api/v1/auth/logout` → clear cookie, return `204`.

**P2-T4: Implement refresh token model**
- Create `V7__create_refresh_tokens.sql` migration (schema in `02-critical-security-risks.md`).
- Create `RefreshTokenEntity.java`, `SpringDataRefreshTokenRepository.java`, `RefreshTokenPersistenceAdapter.java`.
- Create `RefreshTokenUseCase` interface + `RefreshTokenService` implementation.
- Add `POST /api/v1/auth/refresh` endpoint: validates refresh token cookie, issues new access token, rotates refresh token (new token issued, old one deleted — this is refresh token rotation, a PKCE best practice).
- Refresh token cookie: `__Host-refresh`, `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/v1/auth/refresh`, `MaxAge=604800` (7 days).

**P2-T5: Reduce access token lifetime**
- `application.yml`: `jwt.expiration: 900000` (15 minutes, was 86400000).

**P2-T6: Add `password_version` to users table**
- Migration: `ALTER TABLE users ADD COLUMN password_version INT NOT NULL DEFAULT 0;`
- Increment `password_version` in `UserService.updateUserPassword()`.
- Add `pwdVersion` claim to generated JWTs.
- Validate in `JwtCookieAuthFilter`: if `pwdVersion` in token ≠ DB value, return 401.

**P2-T7: Add rate limiting with Bucket4j**
- Add `bucket4j-core` + Caffeine dependencies to `pom.xml`.
- Implement `RateLimitFilter` (10 requests/minute/IP on `/api/v1/auth/**`).
- Implement `RegisterRateLimitFilter` (3 registrations/hour/IP on `POST /api/v1/auth/register`).

**P2-T8: Add security integration tests**
- Test: login sets `__Host-jwt` cookie, no token in body.
- Test: calling protected endpoint with cookie returns 200.
- Test: logout clears cookie.
- Test: 11th login in 1 minute returns 429.
- Test: accessing endpoint with expired token returns 401.
- Test: accessing endpoint after password change returns 401 (pwdVersion mismatch).

### Security Gate 2

- [ ] `curl -v POST /api/v1/auth/login` — response contains `Set-Cookie: __Host-jwt=...; HttpOnly; Secure; SameSite=Strict`.
- [ ] Response body of `/login` contains no JWT string.
- [ ] 11th request to `/login` within 60 seconds returns HTTP 429.
- [ ] Password change → old JWT returns 401 within 1 token validation cycle.
- [ ] All existing auth integration tests still pass.
- [ ] OWASP ZAP passive scan on `/api/v1/auth/login` — zero HIGH findings.

---

## Phase 3 — GitHub OAuth Integration

**Objective:** Implement the primary authentication path for the developer/open-source community.

### Tasks

**P3-T1: Add OAuth2 dependency**
- Add `spring-boot-starter-oauth2-client` to `pom.xml`.

**P3-T2: Add DB columns for OAuth**
- Migration: `V8__add_oauth_support.sql` (schema in `01-v1-gap-analysis.md`).

**P3-T3: Create `OAuthProviderPort`**
- File: `application/auth/port/out/OAuthProviderPort.java`
- Method: `OAuthUserInfo fetchUserInfo(String authorizationCode, String redirectUri)`.
- `OAuthUserInfo` record: `githubId`, `email`, `login`, `avatarUrl`, `name`.

**P3-T4: Implement `GitHubOAuthAdapter`**
- File: `infrastructure/out/oauth/GitHubOAuthAdapter.java`
- Implements `OAuthProviderPort`.
- Step 1: POST to `https://github.com/login/oauth/access_token` with `client_id`, `client_secret`, `code`.
- Step 2: GET `https://api.github.com/user` with the access token.
- Use Spring's `RestClient` (Spring 6+, replaces `RestTemplate`).
- Read `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` from environment variables.
- **Never hardcode OAuth credentials.**

**P3-T5: Create `OAuthLoginUseCase` + `OAuthLoginService`**
- Input: `OAuthCallbackCommand(code, state, redirectUri)`.
- Logic:
  1. Validate `state` parameter (CSRF check — compare with session-scoped value).
  2. Fetch `OAuthUserInfo` from `GitHubOAuthAdapter`.
  3. Look up user by `github_id`. If found → issue tokens. If not found → auto-register, then issue tokens.
  4. Auto-registered users get role `DEVELOPER` by default (configurable).

**P3-T6: Create `OAuthCallbackController`**
- `GET /api/v1/auth/oauth2/callback/github`
- Accepts `code` and `state` query params.
- Returns the same cookie-based auth response as the local login.

**P3-T7: Create `GET /api/v1/auth/oauth2/authorize/github` endpoint**
- Returns the GitHub authorization URL with the `state` parameter.
- `state` must be a cryptographically random value stored server-side (in a short-lived session or in the response cookie for stateless validation).

**P3-T8: Integration tests for OAuth flow**
- Mock `GitHubOAuthAdapter` (don't call real GitHub in tests).
- Test: valid code → user created → cookies set.
- Test: repeated login with same `github_id` → existing user authenticated (no duplicate).
- Test: invalid/replayed `state` → 400 Bad Request.

### Security Gate 3

- [ ] `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` are read from environment only. Zero hardcoded values.
- [ ] `state` parameter validation is enforced. Replaying an old `state` returns 400.
- [ ] A user authenticating via GitHub for the first time receives a valid session cookie.
- [ ] The same `github_id` authenticating twice maps to the same `User.id`.
- [ ] All OAuth integration tests pass.

---

## Phase 4 — Diagram Cloud Sync

**Objective:** Implement the core product feature — the reason the backend exists.

### Tasks

**P4-T1: Create Flyway migrations**
- `V9__create_diagrams.sql` — full schema in `01-v1-gap-analysis.md`.
- Includes `diagrams`, `diagram_collaborators` tables.
- GIN index on `diagrams.content` for JSONB search.

**P4-T2: Create `Diagram` domain aggregate**
- File: `domain/model/Diagram.java`
- Fields: `id`, `ownerId`, `title`, `diagramType` (enum), `content` (Jackson `ObjectNode`), `isPublic`, `version` (long), `createdAt`, `updatedAt`.
- `DiagramType` enum: `CLASS`, `SEQUENCE`, `USECASE`, `ACTIVITY`, `COMPONENT`, `STATE`, `ER`.
- Methods with authorization: `updateContent(UUID requesterId, ObjectNode)`, `makePublic(UUID)`, `makePrivate(UUID)`.
- Factory method: `Diagram.create(UUID ownerId, String title, DiagramType type, ObjectNode content)`.

**P4-T3: Create application use cases**
- `SaveDiagramUseCase` — create new or update existing diagram (auth: owner only for update).
- `GetDiagramUseCase` — get by ID (auth: owner, collaborator, or public).
- `ListDiagramsUseCase` — paginated list of caller's diagrams.
- `ShareDiagramUseCase` — toggle public, add/remove collaborator.
- `DeleteDiagramUseCase` — soft-delete (deactivate).
- Commands: `SaveDiagramCommand(ownerId, title, diagramType, contentJson, clientVersion)`.

**P4-T4: Implement persistence layer**
- `DiagramEntity.java` with `@Version long version` for optimistic locking.
- `ContentJsonConverter.java` — JPA `AttributeConverter<ObjectNode, String>` to store Jackson `ObjectNode` as PostgreSQL `JSONB`.
- `SpringDataDiagramRepository.java` with `findAllByOwnerId(UUID, Pageable)`.
- `DiagramPersistenceAdapter.java`.
- `DiagramPersistenceMapper.java`.

**P4-T5: Handle optimistic locking conflicts**
- Catch `OptimisticLockingFailureException` in `GlobalControllerAdvice`.
- Return `HTTP 409 Conflict` with a body indicating the `version` mismatch.
- Document the client-side conflict resolution protocol.

**P4-T6: Create `DiagramController`**
- `POST /api/v1/diagrams` — create.
- `PUT /api/v1/diagrams/{id}` — update (requires `version` in request body for optimistic lock).
- `GET /api/v1/diagrams/{id}` — get (public diagrams accessible without auth).
- `GET /api/v1/diagrams` — list caller's diagrams.
- `DELETE /api/v1/diagrams/{id}` — soft-delete.
- `PATCH /api/v1/diagrams/{id}/visibility` — toggle public/private.

**P4-T7: Add JSONB content size limit**
- Validate in `SaveDiagramUseCase` that `content` JSON does not exceed 5MB before persisting.
- This prevents storage abuse from malicious or runaway clients.

**P4-T8: Integration tests**
- Create → fetch → update → fetch (verify version increment).
- Concurrent update → 409 Conflict.
- Unauthenticated fetch of public diagram → 200.
- Unauthenticated fetch of private diagram → 401.
- Non-owner update → 403.

### Security Gate 4

- [ ] Optimistic locking test: two concurrent updates to the same diagram — exactly one succeeds with 200, the other gets 409.
- [ ] Authorization test: user B cannot update user A's private diagram → 403.
- [ ] Public diagram: unauthenticated GET returns 200. Unauthenticated PUT returns 401.
- [ ] Content size test: sending 6MB JSON returns 413 or 400.
- [ ] All diagram integration tests pass.

---

## Phase 5 — Observability (Metrics, Tracing, Structured Logging)

**Objective:** Make the backend production-observable. This phase has no security gate of its own but is required before Phase 6.

### Tasks

**P5-T1: Add Actuator + Prometheus**
- Add `spring-boot-starter-actuator` and `micrometer-registry-prometheus` to `pom.xml`.
- Configure `management.endpoints.web.base-path=/internal` with IP allowlisting or token auth.
- Expose only `health`, `info`, `metrics`, `prometheus`.

**P5-T2: Add OpenTelemetry tracing**
- Add `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp`.
- Configure `OTEL_EXPORTER_OTLP_ENDPOINT` from environment.

**P5-T3: Add structured JSON logging**
- Add `logstash-logback-encoder`.
- Create `logback-spring.xml` with JSON appender for `prod` profile.

**P5-T4: Add custom business metrics via `MetricsPort`**
- Create `MetricsPort` outbound port + `MicrometerMetricsAdapter`.
- Inject into: `UserService` (registration counter, login counter), `DiagramService` (save counter, type distribution gauge), `AuthController` (failed login counter).

---

## Phase 6 — Final Security Audit & Public Release Preparation

**Objective:** The last gate before the repository becomes public.

### Tasks

**P6-T1: Full OWASP ZAP active scan**
- Run OWASP ZAP spider + active scan against the full running API.
- All HIGH and CRITICAL findings must be remediated before continuing.
- MEDIUM findings must be documented with a remediation timeline.

**P6-T2: Dependency vulnerability scan**
- Run `./mvnw dependency-check:check` (OWASP Dependency-Check).
- All CRITICAL CVEs in direct or transitive dependencies must be resolved (upgrade or suppress with documented justification).

**P6-T3: Secrets scan**
- Run `trufflesecurity/trufflehog` or `gitleaks` on the full git history.
- Any detected secret must be rotated (not just removed from code — the git history is public).

**P6-T4: Complete API documentation**
- Add `springdoc-openapi-starter-webmvc-ui`.
- Annotate all controllers with `@Operation`, `@ApiResponse`.
- Verify the Swagger UI at `/swagger-ui.html` documents every endpoint correctly.

**P6-T5: Write `SECURITY.md`**
- Document the responsible disclosure process.
- Document known limitations of the current security model.
- Provide a `security@libreuml.io` contact (or GitHub Security Advisories URL).

**P6-T6: Write contributing guide**
- Document the architecture rules (Hexagonal structure, no JPA in domain, etc.).
- Document the security requirements for PRs (no hardcoded values, no new endpoints without auth tests).

### Final Security Gate (Public Release Checklist)

- [ ] Phase 0 Security Gate: PASS
- [ ] Phase 1 Security Gate: PASS
- [ ] Phase 2 Security Gate: PASS
- [ ] Phase 3 Security Gate: PASS
- [ ] Phase 4 Security Gate: PASS
- [ ] OWASP ZAP scan: zero HIGH/CRITICAL findings.
- [ ] Dependency-Check: zero CRITICAL CVEs unresolved.
- [ ] `trufflehog`/`gitleaks` scan: zero secrets detected.
- [ ] `./mvnw test` on clean checkout with only env variables set: 100% tests pass.
- [ ] `SECURITY.md` exists and is accurate.
- [ ] `README.md` documents all required environment variables with instructions for generating secure values.
- [ ] OpenAPI documentation covers 100% of endpoints.
- [ ] No `TODO`, `FIXME`, `HACK`, or `password` literals in committed source code.

---

## Summary Timeline

| Phase | Focus | Prerequisite |
|---|---|---|
| **Phase 0** | Security hardening of existing code | None — start immediately |
| **Phase 1** | Fix broken Q&A persistence | Phase 0 Security Gate |
| **Phase 2** | JWT cookie + rate limiting | Phase 0 Security Gate |
| **Phase 3** | GitHub OAuth | Phase 2 Security Gate |
| **Phase 4** | Diagram Cloud Sync | Phase 2 Security Gate |
| **Phase 5** | Observability | Phases 3 & 4 |
| **Phase 6** | Final audit + public release prep | Phase 5 |

Phases 3 and 4 can be developed in parallel once Phase 2's security gate passes, as they have no shared dependencies.
