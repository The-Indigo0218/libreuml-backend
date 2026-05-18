# LibreUML Backend

REST API for **LibreUML Academy and Cloud Services** — a platform for UML diagram creation, educational course management, and collaborative learning.

Built with **Spring Boot 3.5.10**, Java 21, and PostgreSQL, following a **hexagonal architecture** (ports & adapters).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| Security | JWT (jjwt 0.12.6), HTTP-only cookies |
| Auth | Email/password + OAuth 2.0 (GitHub, Google) |
| Rate limiting | Bucket4j + Caffeine |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI (Swagger UI at `/api/docs`) |
| Observability | Spring Actuator + OpenTelemetry |
| Containerization | Docker + Docker Compose |

---

## Architecture

The project follows hexagonal architecture with three layers:

```
domain/          Pure Java — entities, value objects, domain rules
application/     Use cases, ports (in/out), services
infrastructure/  Spring Boot adapters — REST controllers, JPA, security filters
```

---

## Getting Started

### Prerequisites

- Docker and Docker Compose

### Run with Docker

```bash
docker compose up -d
```

The API starts at `http://localhost:8080`.  
Swagger UI is available at `http://localhost:8080/api/docs`.

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `JWT_SECRET` | Yes | `dev-secret-key-...` | Secret for JWT signing |
| `ADMIN_EMAIL` | Yes | `admin@libreuml.local` | Seed admin email |
| `ADMIN_PASSWORD` | Yes | `Admin1234!` | Seed admin password |
| `DB_NAME` | No | `libreumldb` | PostgreSQL database name |
| `DB_USER` | No | `postgres` | PostgreSQL user |
| `DB_PASSWORD` | No | `password` | PostgreSQL password |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:5173` | Allowed CORS origins |
| `FRONTEND_URL` | No | `http://localhost:5173` | Frontend URL for redirects |
| `GITHUB_CLIENT_ID` | No | — | OAuth GitHub client ID |
| `GITHUB_CLIENT_SECRET` | No | — | OAuth GitHub client secret |
| `GOOGLE_CLIENT_ID` | No | — | OAuth Google client ID |
| `GOOGLE_CLIENT_SECRET` | No | — | OAuth Google client secret |

> **Production:** always set `JWT_SECRET` to a strong random value and override the admin credentials.

### Run locally (without Docker)

```bash
# Start only the database
docker compose up -d db

# Run the application
./mvnw spring-boot:run
```

---

## API Endpoints

| Module | Base path |
|--------|-----------|
| Authentication | `/api/v1/auth` |
| OAuth | `/api/v1/oauth` |
| Users | `/api/v1/users` |
| Courses | `/api/v1/courses` |
| Enrollments | `/api/v1/courses/{id}/join` |
| Course Resources | `/api/v1/courses/{id}/resources` |
| Resources | `/api/v1/resources` |
| Diagrams | `/api/v1/diagrams` |
| Reports | `/api/v1/reports` |

Full interactive documentation: `http://localhost:8080/api/docs`

---

## Security

- JWT tokens issued via HTTP-only cookies (15-minute expiration)
- Password version tracking for immediate token invalidation on password change
- Rate limiting: 10 req/min on auth endpoints, 30 req/min on admin report endpoints (Bucket4j + Caffeine, `maximumSize` bounded)
- Ownership validation on all sensitive endpoints
- OAuth 2.0 support for GitHub and Google
- `sortBy` parameters validated against an allowlist to prevent injection

---

## Database Migrations

Managed with Flyway. Migrations are located in `src/main/resources/db/migration/`.

| Version | Description |
|---------|-------------|
| V1 | Users table |
| V2 | Education core (courses, resources, questions, answers) |
| V3 | Tags on resources |
| V4 | Enrollments |
| V5 | Reports |
| V6 | Refresh tokens and password version |
| V7 | OAuth support |
| V8 | Diagrams and collaborators |

---

## Tests

```bash
./mvnw test
```

**90 tests** — unit tests (Mockito) and integration tests (Testcontainers + PostgreSQL).

Test coverage includes: Auth security, OAuth flow, Diagrams, Reports (with ownership validation), CourseService, EnrollmentService.

---

## Security Audit — May 2026

A full code audit was performed in May 2026. All identified issues were resolved before the first production release.

### Critical (resolved)
- **IDOR in `ReportController`** — any authenticated user could read any report by ID. Fixed by adding ownership validation: only the report owner or an admin can access it.
- **CVE-2023-52422 (jjwt)** — updated from 0.11.5 to 0.12.6.

### High (resolved)
- **JWT invalidation** — verified that `passwordVersion` in the token payload provides immediate invalidation on password change. No blacklist needed.
- **Audit logging** — added structured logs to all admin operations in `ReportController` (status change, priority change, admin response).
- **Rate limiting on admin report endpoints** — extended `RateLimitFilter` to cover `GET /api/v1/reports` and `PATCH` status/priority/respond.
- **Bugs in `ReportWebMapper` and `UserPersistenceMapper`** — fixed missing field mappings and Hibernate proxy handling that caused 500 errors on report endpoints.

### Medium (resolved)
- **Cache memory bounds** — added `maximumSize(10000)` to `authBuckets` in `RateLimitFilter`. Both auth and admin caches are now bounded.
- **`sortBy` injection** — `PaginationCommand` now validates `sortBy` against an allowlist (`createdAt`, `updatedAt`, `title`, `position`, `priority`, `status`, `type`).
- **Input length validation** — added `@NotBlank` and `@Size` constraints to all DTOs that were missing them: `CreateCourseRequest`, `CreateReportRequest`, `CreateResourceRequest`, `ResponseReportRequest`, `UpdateTitleAndDescriptionCourseRequest`, `UpdateVisibilityRequest`, `UpdateTitleAndContentRequest`.
- **Public gallery endpoints** — `GET /api/v1/courses`, `/search`, `/tag/{tag}`, `/slug/{slug}` and `GET /api/v1/diagrams/public` were missing. Implemented full stack (repository → service → controller) including fixing 4 stub `return null` methods in `CoursePersistenceAdapter`.
- **Test coverage** — `CourseServiceTest` expanded from 2 to 17 cases; `EnrollmentServiceTest` created from scratch with 9 cases.

### Low (N/A)
- Deprecated endpoints in `DiagramController` — already clean, no deprecated code found.
- Sensitive data in logs — `ApiKeyAuthenticationFilter` referenced in the audit does not exist; all log statements reviewed and confirmed safe.
