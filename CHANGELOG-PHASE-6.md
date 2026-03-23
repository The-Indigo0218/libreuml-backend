# Phase 6 — Final Audit & Handover: Study Guide & Changelog

## What Was Done

Phase 6 closes the development cycle with two parallel tracks:

| Track | Goal | Outcome |
|---|---|---|
| **OpenAPI / Swagger UI** | Self-documenting API for frontend teams and MCP agents | Swagger UI at `/api/docs`, JSON spec at `/api/api-docs` |
| **OWASP Dependency Audit** | Detect and remediate known CVEs before deployment | 1 High (CVSS 7.5) fixed; 3 low/medium informational |

---

## 1. OpenAPI / Swagger UI Integration

### Why springdoc-openapi

`springdoc-openapi-starter-webmvc-ui` is the de-facto standard for OpenAPI 3.1 documentation in Spring Boot 3.x.  It auto-generates the spec by scanning `@RestController` classes, `@RequestMapping` annotations, request/response DTO types, and Bean Validation constraints — zero manual annotation required to get a useful baseline document.

### Dependency Added

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

springdoc is not in the Spring Boot BOM, so an explicit version is required. Version 2.x targets Spring Boot 3.x / Spring Framework 6.x; version 1.x is for Spring Boot 2.x and is incompatible.

### URL Configuration

```yaml
# application.yml
springdoc:
  swagger-ui:
    path: /api/docs          # Swagger UI served at /api/docs
  api-docs:
    path: /api/api-docs      # OpenAPI JSON served at /api/api-docs
```

Placing both paths under `/api/` keeps all API-facing resources under the same URL namespace. It also means the CorsConfigurationSource already maps `"/api/**"` to allow cross-origin access, so the Swagger UI works from any origin in the allowed list without a separate CORS rule.

Default springdoc paths (`/swagger-ui.html`, `/v3/api-docs`) are **not** exposed — the customised paths are the only active routes.

### Security — Why `permitAll()` Is Required

The `JwtCookieAuthFilter` runs on every request before Spring Security's `authorizeHttpRequests` evaluation.  Without explicit exemptions, a request to `/api/docs` would reach the filter, find no token, and proceed to the authorization stage where `.anyRequest().authenticated()` would reject it with 401 — even though the documentation itself contains no sensitive data.

```java
// SecurityConfig.java — authorizeHttpRequests block
.requestMatchers("/api/docs/**", "/api/docs.html").permitAll()
.requestMatchers("/api/api-docs/**").permitAll()
```

The wildcard patterns are necessary because springdoc mounts several sub-paths:

| Path | Content |
|---|---|
| `GET /api/docs` | Redirect → `/api/docs/index.html` |
| `GET /api/docs/**` | Swagger UI static assets (JS, CSS, favicon) |
| `GET /api/api-docs` | OpenAPI 3.1 JSON document (full spec) |
| `GET /api/api-docs/swagger-config` | springdoc UI bootstrap config |

Without the wildcard on `/api/docs/**`, the browser loads the HTML but then the JS fetch for `/api/docs/swagger-config` gets a 401, and the UI renders blank.

### Production Note

In a production environment where the Swagger UI should not be publicly accessible, add a Spring profile condition:

```java
@Bean
@ConditionalOnExpression("${springdoc.swagger-ui.enabled:true}")
```

Or set `springdoc.swagger-ui.enabled=false` / `springdoc.api-docs.enabled=false` in a `prod` profile application.yml.  The security rules already apply to all profiles; the toggle just prevents the spec from being generated at all.

---

## 2. OWASP Dependency-Check Audit

### How It Was Run

```bash
./mvnw org.owasp:dependency-check-maven:check \
  -DnvdApiDelay=6000 \     # 6 s between NVD API calls — respects unauthenticated rate limit
  -DfailBuildOnCVSS=7 \    # fail build on any CVSS ≥ 7.0 (High / Critical)
  -DretireJsAnalyzerEnabled=false \
  -DnodeAnalyzerEnabled=false
```

`-DfailBuildOnCVSS=7` mirrors the industry standard "shift-left security gate": anything with a CVSS High (7.0–8.9) or Critical (9.0–10.0) score blocks the build, forcing a fix before merge.  CVSS Medium (4.0–6.9) and Low (0.1–3.9) findings are reported but do not block.

> **Note on NVD rate limiting:** without an API key, the NVD API is capped at 5 requests per 30 seconds.  `-DnvdApiDelay=6000` paces requests to 1 every 6 seconds to stay within that limit.  For CI/CD pipelines set `NVD_API_KEY` in the environment and remove the delay flag.

---

## 3. Audit Results

### 3a. HIGH — Build-Breaking Finding (FIXED)

| Field | Value |
|---|---|
| **CVE** | CVE-2026-24734 |
| **CVSS** | 7.5 (High) |
| **Artifact** | `tomcat-embed-core-10.1.50` |
| **Root cause** | Spring Boot 3.5.10 ships Tomcat 10.1.50, which is vulnerable to CVE-2026-24734 |
| **Fix** | Override `<tomcat.version>10.1.52</tomcat.version>` in `pom.xml` properties |

**Why the fix works:** Spring Boot's parent POM defines all managed dependency versions as Maven properties.  Setting `<tomcat.version>` in our project properties overrides the BOM value without forking the parent.  All three `tomcat-embed-*` artifacts (`core`, `websocket`, `el`) are governed by the same property and are upgraded atomically.

```xml
<!-- pom.xml <properties> block -->
<tomcat.version>10.1.52</tomcat.version>
```

After the fix, the OWASP check exited with **BUILD SUCCESS** and no `tomcat-embed-core` CVE was reported.

---

### 3b. LOW / MEDIUM — Informational Findings (Not Fixed)

These findings appeared in the warning output but did **not** fail the build (CVSS < 7.0).  They are documented here for transparency.

| CVE | Artifact | CVSS | Ownership | Action |
|---|---|---|---|---|
| CVE-2025-48924 | `commons-lang3-3.17.0` | < 7.0 (Medium) | Spring Boot BOM — transitive | No direct version available; monitor Spring Boot BOM updates |
| CVE-2020-29582 | `kotlin-stdlib-1.9.25` | 5.3 (Medium) | Transitive via Testcontainers — `test` scope only | Test-scope-only; no runtime exposure; Kotlin stdlib is managed by Testcontainers' own BOM |
| CVE-2025-68161 | `log4j-api-2.24.3` | < 7.0 (Medium) | Spring Boot BOM — transitive (not a direct dep) | No runtime log4j usage (project uses Logback); API jar is on classpath only as a bridge; monitor Spring Boot BOM updates |

**Key reasoning for not fixing these:**

1. **BOM-managed transitive dependencies:** `commons-lang3` and `log4j-api` versions are fully controlled by the Spring Boot parent BOM.  Manually pinning them would diverge from the BOM, risk binary incompatibilities, and require ongoing manual maintenance.  The correct fix is to upgrade the Spring Boot parent when a new BOM version ships with patched transitive versions.

2. **Test-scope isolation:** `kotlin-stdlib` is pulled in by Testcontainers only during test execution.  It is not present in the production JAR and is never loaded at runtime in a deployed environment.

3. **CVSS < 7.0:** None of these meet the project's security gate threshold.  They represent accepted risk at the current severity level.

---

## 4. Files Created / Modified

### New Files

| File | Purpose |
|---|---|
| `CHANGELOG-PHASE-6.md` | This study guide |

### Modified Files

| File | Change |
|---|---|
| `pom.xml` | Added `springdoc-openapi-starter-webmvc-ui:2.8.8`; added `<tomcat.version>10.1.52</tomcat.version>` to fix CVE-2026-24734 |
| `src/main/resources/application.yml` | Added `springdoc.swagger-ui.path=/api/docs` and `springdoc.api-docs.path=/api/api-docs` |
| `SecurityConfig.java` | Added `permitAll()` for `/api/docs/**`, `/api/docs.html`, `/api/api-docs/**` |

---

## 5. API Documentation — Quick Reference

Once the application is running locally:

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/api/docs` |
| OpenAPI JSON | `http://localhost:8080/api/api-docs` |
| OpenAPI YAML | `http://localhost:8080/api/api-docs.yaml` |

The JSON spec can be imported directly into Postman, Insomnia, or any OpenAPI-compatible toolchain.  MCP agents can fetch `GET /api/api-docs` to discover all available endpoints programmatically.

---

## 6. Final Test Results

```
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

All 57 tests pass with Tomcat 10.1.52, springdoc on the classpath, and the updated SecurityConfig rules.  No existing test was broken by any Phase 6 change.
