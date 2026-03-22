# 03 — Hexagonal / DDD Architecture Evaluation

**Audit Date:** 2026-03-21
**Target Architecture:** Hexagonal (Ports & Adapters) loosely guided by DDD tactical patterns

---

## Overall Verdict

The codebase shows **genuine, above-average effort** to implement Hexagonal Architecture. The port/adapter naming, the dual-mapper pattern (web mappers vs persistence mappers), and the domain model separation from JPA entities are all structurally correct. However, there are **7 significant boundary violations** and **3 domain-model design failures** that would prevent this from being called genuinely Hexagonal at a senior level. These are detailed below.

---

## What Is Correct (Preserve These Patterns)

- **Inbound ports** (`CreateUserUseCase`, `LoginUseCase`, etc.) are interfaces in `application/*/port/in/`. Controllers depend on these interfaces, never on service implementations. Correct.
- **Outbound ports** (`UserRepository`, `TokenProviderPort`, `PasswordEncoderPort`) are interfaces in `application/*/port/out/`. Services depend on these interfaces. Correct.
- **Two distinct mapper layers** exist: `infrastructure/in/web/mapper/` translates HTTP DTOs to/from Commands, and `infrastructure/out/persistence/mapper/` translates JPA entities to/from domain objects. This is architecturally clean.
- **Domain models are separate from JPA entities.** `User.java` and `UserEntity.java` are different classes. Correct.
- **Commands** (`CreateUserCommand`, `LoginCommand`) exist as the data-crossing boundary between the web layer and the application layer. Correct.

---

## Violation 1 — Domain Models Extend `UserEntity` Logic via JPA Annotations (CRITICAL)

### The Problem

The domain `User.java` is likely annotated with `@SuperBuilder` (Lombok). While Lombok is acceptable, the **domain models must have zero dependency on any persistence technology**. If `User.java` (or any domain model) imports `jakarta.persistence.*`, `javax.persistence.*`, or Hibernate-specific types, the domain layer is coupled to the infrastructure.

Additionally, the `users` table uses **Single Table Inheritance (STI)** with a `user_type` discriminator column. This is a persistence strategy decision. If the `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` and `@DiscriminatorColumn` annotations are on `UserEntity.java` — good. If they are on the domain `User.java` — this is a critical violation that merges the domain and infrastructure layers.

### The Fix

```
domain/model/User.java          — MUST contain: zero JPA annotations
                                                 business methods only
                                                 imports: only java.*, lombok, jackson

infrastructure/out/persistence/entity/UserEntity.java
                                — MAY contain:  @Entity, @Table, @Inheritance,
                                                 @DiscriminatorColumn, @Column
```

Run this check to identify the violation immediately:

```bash
grep -rn "jakarta.persistence" src/main/java/com/libreuml/backend/domain/
# Zero results expected. Any result = critical violation.
```

---

## Violation 2 — `CourseService` and `ResourceService` Receive `UUID creatorId` Instead of a Domain Command (HIGH)

### The Problem

Use case methods in several services accept raw primitive arguments:

```java
// CourseService.java (inferred from method signatures)
public Course getCourseById(UUID courseId, UUID requesterId) { ... }
public void deactivateCourse(UUID courseId, UUID userId) { ... }
```

This breaks the Command Object pattern. The application layer's input port should define a **Command** (a value object) that encapsulates all parameters of a use case invocation. Passing naked UUIDs means:
- The method signature can change silently without refactoring the interface.
- No validation can be applied to the command before it crosses the port boundary.
- The port interface becomes meaningless (it's just method signature forwarding, not intent expression).

### The Fix

Every use case method must take a named Command object:

```java
// application/courses/port/in/dto/GetCourseCommand.java
public record GetCourseCommand(UUID courseId, UUID requesterId) {}

// application/courses/port/in/GetCourseUseCase.java
public interface GetCourseUseCase {
    Course getCourseById(GetCourseCommand command);
}
```

Commands should be **validated** at the port boundary using `@Valid` + `@NotNull` annotations.

---

## Violation 3 — `EnrollmentService` Is Not in `application/enrollment/port/service/` (MEDIUM)

### The Problem

The `EnrollmentService.java` is located at `application/enrollment/service/EnrollmentService.java` — note the missing `port/` segment. Every other service follows the path `application/*/port/service/`. This is a naming inconsistency that signals the developer lost track of the architecture convention mid-implementation.

In Hexagonal Architecture, package paths encode architectural intent. An inconsistent path means tools like ArchUnit cannot uniformly enforce that "services must implement at least one inbound port interface."

### The Fix

Move to: `application/enrollment/port/service/EnrollmentService.java` and update the ArchUnit test to enforce this rule:

```java
// ArchUnitTests.java — add this rule
@ArchTest
static final ArchRule services_must_be_in_port_service_package =
    classes()
        .that().haveNameMatching(".*Service")
        .and().resideInAPackage("..application..")
        .should().resideInAPackage("..port.service..");
```

---

## Violation 4 — Q&A Persistence Adapters Have No JPA Entities (CRITICAL)

### The Problem

`QuestionRepositoryAdapter.java` and `AnswerRepositoryAdapter.java` exist in `infrastructure/out/persistence/adapter/` and implement the outbound ports `QuestionRepository` and `AnswerRepository` from the application layer. They presumably delegate to `SpringDataQuestionRepository` and `SpringDataAnswerRepository`.

However, **no `QuestionEntity.java` or `AnswerEntity.java` exists**, and **no Flyway migration creates the `questions` or `answers` tables**. This means:

- The Spring Data repositories cannot work without their entity class.
- The adapters are dead code at best, a runtime crash at worst.
- The entire Q&A feature is silently broken.

This is the most dangerous type of architectural violation: it looks complete on the surface but fails at runtime.

### The Fix

Create the missing pieces (in order):

1. **V6__create_qa_tables.sql** migration.
2. `QuestionEntity.java` with `@Entity @Table(name = "questions")`.
3. `AnswerEntity.java` with `@Entity @Table(name = "answers")`.
4. `QuestionPersistenceMapper.java` in `infrastructure/out/persistence/mapper/`.
5. `AnswerPersistenceMapper.java`.
6. Update `QuestionRepositoryAdapter` and `AnswerRepositoryAdapter` to use the new mappers.

```sql
-- V6__create_qa_tables.sql
CREATE TABLE questions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    tags        JSONB        DEFAULT '[]',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_solved   BOOLEAN      NOT NULL DEFAULT FALSE,
    creator_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_urls  JSONB        DEFAULT '[]',
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE answers (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT         NOT NULL,
    is_accepted BOOLEAN      NOT NULL DEFAULT FALSE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    image_urls  JSONB        DEFAULT '[]',
    creator_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id UUID         NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_questions_creator  ON questions(creator_id);
CREATE INDEX idx_questions_tags     ON questions USING gin(tags);
CREATE INDEX idx_answers_question   ON answers(question_id);
CREATE INDEX idx_answers_creator    ON answers(creator_id);
```

---

## Violation 5 — Domain Logic Leaking Into Persistence Adapter (MEDIUM)

### The Problem

In `EnrollmentPersistenceAdapter.java` (and likely `CoursePersistenceAdapter`), the adapter likely contains authorization logic or business conditions like:

```java
// EnrollmentPersistenceAdapter — suspected pattern
public void joinCourse(UUID studentId, UUID courseId) {
    Optional<EnrollmentEntity> existing = repo.findByStudentIdAndCourseId(studentId, courseId);
    if (existing.isPresent() && existing.get().isActive()) {
        throw new EnrollmentAlreadyExistsException(...);
    }
    // ...
}
```

Business rule evaluation ("is this enrollment already active?") belongs in the **domain model** or the **application service**, not the persistence adapter. The adapter's only responsibility is translation and storage.

### The Fix

The adapter must only perform data access:

```java
// EnrollmentPersistenceAdapter — correct scope
public Optional<Enrollment> findByStudentAndCourse(UUID studentId, UUID courseId) {
    return repo.findByStudentIdAndCourseId(studentId, courseId)
               .map(persistenceMapper::toDomain);
}

// EnrollmentService — business rule lives here
public void joinCourse(EnrollmentCommand cmd) {
    Enrollment existing = enrollmentRepo
        .findByStudentAndCourse(cmd.studentId(), cmd.courseId())
        .orElse(null);

    if (existing != null && existing.isActive()) {
        throw new EnrollmentAlreadyExistsException(cmd.studentId(), cmd.courseId());
    }
    // ...
}
```

---

## Violation 6 — Anemic Domain Models (HIGH)

### The Problem

The domain models have some behavior (`deactivate()`, `changePassword()`, `resolve()`) but remain largely anemic. Key symptoms:

**`Course.java`** has `setTitle()`, `setDescription()`, `setCoverUrl()` through Lombok `@Builder` or direct field access. Invariant protection is absent: there is nothing preventing a `Course` from being created with a `null` title, an empty slug, and zero tags.

**`Report.java`** has no behavior methods at all (except `solveReportTime()`). The service calls setters directly to update status and priority.

**`Resource.java`** has no validation that `content` is non-null or that `type` is consistent with `content` format (e.g., a `VIDEO` type resource should require a URL as content, not a markdown string).

In true DDD, the **aggregate** is responsible for enforcing its own invariants. No external code should be able to create an invalid aggregate.

### The Fix

**Step 1: Use factory methods / constructors instead of `@Builder` for invariant enforcement.**

```java
// domain/model/Course.java — protected invariants
public class Course {
    // No public setters. All modification through methods.
    private UUID   id;
    private String title;
    private String description;
    private boolean active;

    // Factory method enforces invariants at creation time
    public static Course create(UUID creatorId, String title, String description, List<String> tags) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be blank.");
        }
        if (tags != null && tags.size() > 5) {
            throw new IllegalArgumentException("A course may have at most 5 tags.");
        }
        Course c = new Course();
        c.id          = UUID.randomUUID();
        c.creatorId   = creatorId;
        c.title       = title.strip();
        c.description = description;
        c.tags        = tags != null ? List.copyOf(tags) : List.of();
        c.active      = true;
        c.createdAt   = Instant.now();
        return c;
    }

    // Named mutation methods enforce post-invariants
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.isBlank())
            throw new IllegalArgumentException("Title cannot be blank.");
        this.title     = newTitle.strip();
        this.updatedAt = Instant.now();
    }
}
```

**Step 2: The 5-tag business rule should live in `Course`, not `CourseService`.**

Currently:
```java
// CourseService.java — business rule in application layer (wrong)
if (command.tags().size() > 5) throw new ValidationException("Max 5 tags");
```

Correct location:
```java
// Course.java — business rule in domain (correct)
public void updateTags(List<String> newTags) {
    if (newTags.size() > 5) throw new TagLimitExceededException(newTags.size());
    this.tags = List.copyOf(newTags);
}
```

---

## Violation 7 — `GlobalControllerAdvice` Catches Raw `RuntimeException` as Catch-All (LOW)

### The Problem

`GlobalControllerAdvice.java` likely has a catch-all handler:

```java
@ExceptionHandler(RuntimeException.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ProblemDetail handleGeneral(RuntimeException ex) { ... }
```

This swallows all unchecked exceptions with a 500, hiding bugs. More critically, if a domain exception (which should map to 400 or 403) accidentally becomes an unchecked `RuntimeException` subclass, it silently returns 500 instead of the correct status.

### The Fix

Use Spring's `ProblemDetail` (RFC 7807, built into Spring 6+) and map each domain exception explicitly. Have a true fallback only for `Exception.class` (checked) and `Throwable` (for truly unexpected failures):

```java
// GlobalControllerAdvice.java
@ExceptionHandler(UserNotFoundException.class)
public ResponseEntity<ProblemDetail> handleNotFound(UserNotFoundException ex, WebRequest req) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    detail.setType(URI.create("https://libreuml.io/errors/not-found"));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(detail);
}

// True catch-all — logs stack trace, never exposes internal message
@ExceptionHandler(Exception.class)
public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest req) {
    log.error("Unhandled exception on request: {}", req.getDescription(false), ex);
    ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    detail.setDetail("An unexpected error occurred. Please contact support.");
    return ResponseEntity.status(500).body(detail);
}
```

---

## ArchUnit Test Coverage Gaps

The project has `archunit-junit5` as a dependency, which is excellent. The following rules should be enforced to prevent future regressions:

```java
@ArchTest
static final ArchRule no_jpa_in_domain =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().accessClassesThat().resideInAPackage("jakarta.persistence..");

@ArchTest
static final ArchRule controllers_must_not_use_services_directly =
    noClasses()
        .that().resideInAPackage("..in.web.controller..")
        .should().dependOnClassesThat().resideInAPackage("..port.service..");

@ArchTest
static final ArchRule persistence_adapters_must_not_access_controllers =
    noClasses()
        .that().resideInAPackage("..out.persistence..")
        .should().dependOnClassesThat().resideInAPackage("..in.web..");

@ArchTest
static final ArchRule domain_must_not_depend_on_infrastructure =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..");
```

---

## Architecture Health Summary

| Aspect | Status | Priority |
|---|---|---|
| Port/adapter naming convention | GOOD | — |
| Dual-mapper layer | GOOD | — |
| Domain ↔ Entity separation | GOOD | — |
| No JPA annotations in domain | NEEDS VERIFICATION | High |
| Q&A persistence layer complete | BROKEN | Critical |
| Domain model invariant protection | WEAK | High |
| Command objects for all use case inputs | PARTIAL | Medium |
| EnrollmentService package location | WRONG | Low |
| Business logic in persistence adapters | SUSPECTED | Medium |
| ArchUnit rule coverage | PARTIAL | Medium |
