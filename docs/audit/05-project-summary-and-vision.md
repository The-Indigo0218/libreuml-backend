# 05 — Project Summary & Vision

**Audit Date:** 2026-03-21
**Document Purpose:** Comprehensive technical briefing for AI assistants, new contributors, and stakeholder synchronization.
**Status:** Active development — pre-public release

---

## 1. What LibreUML Is

LibreUML is an **open-source, hybrid local-first / cloud-ready UML editor** built as a desktop application (Electron + React frontend) with a Spring Boot 3 REST API backend. It targets software developers, students, and educators who need professional-grade UML diagramming without vendor lock-in, subscription fees, or mandatory cloud connectivity.

The product operates on three distinct axes:

1. **Editor:** A full-featured UML authoring environment (class, sequence, use case, activity, component, state, ER diagrams) that works entirely offline and exports to XMI, SVG, PNG, and PDF.
2. **Cloud Sync:** An optional backend that stores diagram snapshots as JSONB, enabling cross-device access, diagram sharing, and version history — without requiring a subscription.
3. **Academy:** An integrated educational platform (the LibreUML Academy) that provides structured UML courses, reusable resources, a Q&A forum, and progress tracking — turning LibreUML into a learning ecosystem, not just a tool.

---

## 2. Technology Stack

### Backend

| Layer | Technology | Version |
|---|---|---|
| Runtime | Java | 21 (LTS, preview features enabled) |
| Framework | Spring Boot | 3.5.10 |
| Security | Spring Security | 6.x (ships with Boot 3.5) |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Database | PostgreSQL | 16+ |
| Migrations | Flyway | 10.x |
| Auth | JJWT (HMAC HS512) | 0.11.5 |
| Mapping | MapStruct + Lombok | 1.6.3 / 1.18.x |
| Architecture testing | ArchUnit | 1.2.1 |
| Integration testing | Testcontainers (PostgreSQL) | Latest |
| Build | Maven | 3.x |

### Frontend (out of scope for this audit, documented for context)

| Technology | Role |
|---|---|
| Electron | Desktop shell, offline-first capability |
| React | UI framework |
| TypeScript | Language |
| XMI / SVG export | Diagram interchange formats |

---

## 3. What the Backend Currently Does (Implemented)

### 3.1 Authentication & User Management

The backend provides a complete local authentication system:

- **Registration** (`POST /api/v1/auth/register`): Creates users with roles `TEACHER`, `STUDENT`, or `DEVELOPER`. Role is provided at registration. Email uniqueness is enforced at DB level.
- **Login** (`POST /api/v1/auth/login`): Validates credentials with BCrypt, issues a 24-hour JWT (HS512-signed) in the response body.
- **Profile management** (`PATCH /api/v1/users/*`): Four granular update endpoints for basic info, social links, email, and password. Password change validates the old password before accepting the new one.

The user domain supports **three concrete user types** through Single Table Inheritance (STI) with a `user_type` discriminator column:

- `Student`: Enrolls in courses, participates in Q&A.
- `Teacher`: Creates courses, holds a unique teacher code.
- `Developer`: Full access, can create resources. Represents power users.

Five roles exist: `TEACHER`, `STUDENT`, `DEVELOPER`, `MODERATOR`, `ADMIN`. Role-based access control is enforced via `@PreAuthorize` annotations on sensitive endpoints.

An **admin seeder** (`AdminSeeder`) runs at startup to create the initial admin account from `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables if it does not already exist.

### 3.2 Course Management

Courses are the primary educational unit:

- **Lifecycle:** Create → active, update title/description, update cover image URL, toggle visibility (PUBLIC/PRIVATE), soft-delete (deactivate).
- **Slug generation:** Titles are automatically slugified for URL-friendly course identifiers with collision handling.
- **Tags:** Up to 5 tags per course. Tags are stored as JSONB for flexible querying.
- **Authorization:** Only the creator or an ADMIN can modify or deactivate a course. Other authenticated users can view PUBLIC courses.
- **Pagination:** Public course listing supports pagination with configurable page size, sort field, and direction.

### 3.3 Resources

Resources are reusable learning assets that can be embedded in courses:

- **Types:** `VIDEO`, `LNK` (link), `POST` (markdown content), `FILE`, `DRIVE` (Google Drive embed).
- **Content:** A single `content` text field stores the URL or markdown body depending on type.
- **Tags:** JSONB column, searchable.
- **Operations:** Create, update title/content, update tags, deactivate (soft-delete by creator only).
- **Querying:** Paginated listing with search by title, filter by tag, and filter by creator.

### 3.4 Course Resources (Course-Resource Join)

The `course_resources` table is a join table with enrichment (position, visibility) that links courses to resources:

- Allows a teacher to **add a resource to a course** and assign it a position.
- Supports **reordering** (bulk position update) via `PUT /api/v1/course-resources/{courseId}/updatePositions`.
- Supports **per-resource visibility toggle** within a course.
- Enforces ownership: only the course creator can add/remove/reorder resources.

### 3.5 Enrollments

The enrollment system manages student-course relationships:

- **Join:** A student enrolls in a PUBLIC course or a PRIVATE course they've been given access to.
- **Leave:** Students can unenroll (soft-delete — enrollment is deactivated, not deleted).
- **Re-enrollment:** A previously deactivated enrollment is reactivated instead of creating a duplicate row.
- **Teacher view:** Teachers can view the enrollment details of specific students in their courses.

### 3.6 Reports (Admin Support System)

A multi-type feedback/support ticket system:

- **Types:** `BUG`, `SUGGESTION`, `GRATITUDE`, `CONTACT`.
- **Lifecycle:** `OPEN` → `IN_PROGRESS` → `RESOLVED` / `REJECTED`.
- **Priority:** `LOW`, `MEDIUM`, `HIGH`, `NONE`.
- **Admin operations:** Update status, set priority, write a formal response (which records the `solved_at` timestamp), write internal notes.
- **User operations:** Create a report with optional evidence images (URLs stored as JSONB), view own reports, view a specific report.
- **ADMIN listing:** Paginated, filtered by type, status, and priority.

### 3.7 Q&A System (Application Layer Complete, Persistence Incomplete)

The Q&A domain model and application service layer are implemented:

- **Questions:** Title, markdown content, tags, image attachments (JSONB), active/inactive, solved/unsolved.
- **Answers:** Content, image attachments, accepted/not-accepted, active/inactive.
- **Authorization:** Only the owner, a MODERATOR, or an ADMIN can deactivate a question or answer. Only the question creator can mark an answer as accepted.

**Critical note:** The Q&A persistence layer (JPA entities, Flyway migrations, Spring Data repositories) is **not yet complete**. The service layer exists but has no database backing.

---

## 4. What the Backend Does Not Yet Do (Planned for V1)

### 4.1 GitHub OAuth Authentication

Users will be able to sign in with GitHub. On first OAuth login, a new account is created automatically. On subsequent logins, the user is identified by their stable `github_id`. Teachers and developers who authenticate via GitHub bypass the email/password flow entirely. This is the primary authentication path for the open-source developer community.

### 4.2 JWT HttpOnly Cookie Transport

The current JWT-in-body approach will be replaced with `HttpOnly; Secure; SameSite=Strict` cookies. A short-lived access token (15 min) + long-lived refresh token (7 days, stored in DB) will be implemented. This eliminates XSS-based token theft. A `POST /api/v1/auth/logout` endpoint will clear both cookies server-side.

### 4.3 Diagram Cloud Sync

The flagship feature of LibreUML as a product:

- Users will be able to **save diagrams** from the desktop editor to the cloud backend.
- Diagrams are stored as **JSONB** in PostgreSQL (`diagrams` table), allowing the backend to query, search, and transform diagram content without a fixed schema.
- Supported diagram types: `CLASS`, `SEQUENCE`, `USECASE`, `ACTIVITY`, `COMPONENT`, `STATE`, `ER`.
- **Version history:** Each save creates or updates a version snapshot, enabling basic undo history across sessions.
- **Sharing:** Diagrams can be made public (read-only link) or shared with collaborators (read/edit permission).
- **Conflict resolution:** Optimistic locking via a `version` counter prevents lost-update conflicts when a user has the same diagram open on two devices.
- **Local-first:** The frontend is authoritative for the current session. Sync to the backend is triggered explicitly (auto-save or manual save). Conflict UI is handled in the frontend.

### 4.4 Telemetry & Metrics

- **Spring Boot Actuator** with Prometheus endpoint (`/internal/actuator/prometheus`) for operational metrics.
- **Micrometer** custom counters for business events: user registrations, diagram saves, logins, OAuth logins, export operations.
- **Distributed tracing** via OpenTelemetry for request correlation across services.
- **Structured JSON logging** for log aggregation (ELK stack or Grafana Loki).

---

## 5. LibreUML Academy — Integrated Educational Vision

The Academy is not an add-on — it is co-equal with the editor as a product pillar. The vision is to make LibreUML the canonical place to **learn UML while using UML**.

### Academy Structure

```
Course (e.g., "UML Fundamentals")
├── CourseResource (position 1) → Resource (VIDEO: "Introduction to Class Diagrams")
├── CourseResource (position 2) → Resource (POST: "Class Diagram Reference Guide")
├── CourseResource (position 3) → Resource (LNK: "OMG UML Specification v2.5")
└── CourseResource (position 4) → Resource (FILE: "Practice Exercise 1.xmi")

Enrollment
└── Student → Course (progress tracked, enrolledAt timestamp)

Q&A
└── Question ("How do I model inheritance in a class diagram?")
    └── Answer (by Teacher, marked accepted)
```

### User Roles in the Academy

| Role | Academy Capabilities |
|---|---|
| `STUDENT` | Enroll in courses, view resources, ask/answer questions |
| `TEACHER` | All student capabilities + create/manage courses and resources |
| `DEVELOPER` | All teacher capabilities + access to API/developer resources |
| `MODERATOR` | Manage Q&A (delete inappropriate content, mark solutions) |
| `ADMIN` | Full system access, manage reports, view analytics |

### Academy Roadmap (Post-V1)

- **Progress tracking:** Per-resource completion status. Course completion certificates.
- **Course ratings:** Students rate courses after completion.
- **Teacher analytics:** View enrollment counts, resource engagement metrics.
- **Live diagram exercises:** Embedded LibreUML editor within Academy course pages, with instructor-defined diagram templates as starting points.
- **UML Assessment:** Auto-graded UML exercises where the backend validates student-submitted XMI against a rubric.

---

## 6. Database Schema (Current State)

| Table | Purpose | Key Columns |
|---|---|---|
| `users` | All user types (STI) | `id UUID`, `user_type`, `email UNIQUE`, `role`, `academic_degrees JSONB`, `stacks JSONB` |
| `courses` | Course catalog | `id UUID`, `slug UNIQUE`, `visibility`, `tags JSONB`, `creator_id FK` |
| `resources` | Learning assets | `id UUID`, `type`, `content TEXT`, `tags JSONB`, `creator_id FK` |
| `course_resources` | Course-resource join with position | `(course_id, resource_id) UNIQUE`, `position INT`, `visible BOOL` |
| `enrollments` | Student-course membership | `(student_id, course_id) UNIQUE`, `progress INT`, `active BOOL` |
| `reports` | Support/feedback tickets | `type`, `status`, `priority`, `admin_response TEXT`, `evidences_images JSONB` |
| `questions` | *(migration not yet written)* | — |
| `answers` | *(migration not yet written)* | — |
| `diagrams` | *(not yet designed)* | — |
| `refresh_tokens` | *(not yet designed)* | — |

---

## 7. Architecture Summary

The backend follows **Hexagonal Architecture (Ports & Adapters)** with a 3-layer package structure:

```
com.libreuml.backend/
├── domain/                  ← Business entities and invariants (no framework dependencies)
├── application/             ← Use cases, ports (interfaces), commands, exceptions
│   └── <feature>/
│       └── port/
│           ├── in/          ← Inbound port interfaces (use cases)
│           ├── out/         ← Outbound port interfaces (repositories, external services)
│           └── service/     ← Use case implementations
└── infrastructure/
    ├── in/web/              ← HTTP adapters (controllers, DTOs, web mappers)
    └── out/persistence/     ← JPA adapters (entities, Spring Data repos, persistence mappers)
        └── security/        ← Spring Security adapters (JWT, UserDetails)
```

**Dependency flow:** `infrastructure/in/web` → `application/*/port/in` → `application/*/port/service` → `application/*/port/out` ← `infrastructure/out/persistence`

The domain layer has zero dependency on Spring, Hibernate, or any framework. This is the invariant enforced (partially) by the existing ArchUnit test suite.

---

## 8. Deployment Model

LibreUML is designed to operate in three deployment modes:

| Mode | Backend | Database | Auth |
|---|---|---|---|
| **Fully Local** | Not used | LocalStorage/IndexedDB only | N/A |
| **Self-Hosted** | User runs their own Spring Boot instance | User's PostgreSQL | GitHub OAuth + local |
| **Managed Cloud** | Operated by LibreUML maintainers | Managed PostgreSQL (e.g., Supabase, RDS) | GitHub OAuth + local |

The hybrid local-first model means the desktop app is always functional without a backend. The backend is opt-in for sync, sharing, and Academy features.

---

## 9. Security Model

For a public open-source project, the threat model includes adversaries who can read the full source code. The security posture must assume:

- All algorithm choices, key lengths, and configuration defaults are visible to attackers.
- Any hardcoded secret is a compromised secret.
- All endpoints are probed for injection, privilege escalation, and authentication bypass.

The current implementation addresses several of these concerns correctly (BCrypt passwords, JWT validation, role-based authorization) but has critical gaps documented in `02-critical-security-risks.md`.

---

## 10. Context for AI Assistants

When working on this codebase, keep the following in mind:

1. **Architecture is Hexagonal.** New features must follow the port/adapter pattern. Never add Spring Data repositories directly to controllers or services. Always create an outbound port interface in `application/*/port/out/` and implement it in `infrastructure/out/persistence/adapter/`.

2. **Domain models must be framework-free.** No `@Entity`, `@Column`, `jakarta.persistence.*` in `domain/model/`. JPA annotations belong exclusively in `infrastructure/out/persistence/entity/`.

3. **Mappers are mandatory at both boundaries.** Web request → Command (via web mapper). Domain → Response DTO (via web mapper). Domain ↔ JPA Entity (via persistence mapper). Never bypass mappers.

4. **SQL schema changes go through Flyway.** Never change `ddl-auto` to `create` or `update`. Create a new `V{N}__description.sql` migration file in `src/main/resources/db/migration/`.

5. **Security is open-source threat model.** Assume all source is public. No hardcoded secrets. All auth flows must be audited.

6. **Java 21 features are available and preferred.** Use records for Commands, DTOs, and value objects. Use sealed classes for domain exception hierarchies. Use pattern matching `instanceof`.

7. **The user handles all git commits.** Do not commit. Provide code changes only.
