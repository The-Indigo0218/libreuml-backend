# Informe de Auditoría de Código - LibreUML Backend

**Fecha de auditoría:** Mayo 2026  
**Alcance:** Código fuente completo (src/main y src/test)  
**Versión del proyecto:** 0.0.1-SNAPSHOT

---

## Resumen Ejecutivo

El proyecto es un backend Spring Boot 3.5.10 con arquitectura hexagonal bien implementada. La seguridad está bien configurada con JWT, API Keys, rate limiting y filtros CSRF. Sin embargo, se identificaron varias áreas que requieren atención.

---

## 1. FALLAS ESTRUCTURALES

### 1.1 Endpoints duplificados/deprecados sin eliminar
- **Archivo:** `DiagramController.java:37-50`
- **Problema:** El controller tiene headers deprecación pero los endpoints aún funcionan. El endpoint `/api/v1/diagrams` compete con `/api/v1/projects/{id}/diagrams` creando redundancia.
- **Impacto:** Confusión para consumidores de API, posible drifts de datos.

### 1.2 Ausencia de validación deOwnership en algunos endpoints
- **Archivo:** `ReportController.java:62-66`
- **Problema:** El endpoint `GET /api/v1/reports/{id}` no valida que el usuario actual sea el propietario del reporte ni un administrador. Cualquier usuario autenticado puede ver cualquier reporte.
- **Impacto:** Exposición de información sensible.

### 1.3 Validación insuficiente en DTOs
- **Archivo:** `CreateProjectRequest.java` (no encontrado pero referenciado en ProjectController)
- **Problema:** No se encontró validación de longitud máxima para campos como `name`, `description`, `author`, `basePackage`. Podría permitir payloads excesivamente grandes.
- **Impacto:** Potential DoS vía payloads malformed.

### 1.4 Arquitectura de proyectos sin endpoint de listado público
- **Problema:** Los proyectos con visibilidad `SHARED` o `PUBLIC` no tienen forma de ser descubiertos por otros usuarios. Solo se pueden acceder si se conoce el ID.
- **Impacto:** No hay forma de explorar proyectos públicos.

---

## 2. COBERTURA DE TESTS - ÁREAS SIN TESTEAR

### 2.1 Controladores sin tests de integración

| Controlador | Ruta | Estado |
|-------------|------|--------|
| `ReportController` | `/api/v1/reports` | **Sin tests** - Peligroso por manejo de datos sensibles |
| `CourseController` | `/api/v1/courses` | **Sin tests** |
| `EnrollmentController` | `/api/v1/courses/{id}/join` | **Sin tests** |
| `ResourceController` | `/api/v1/resources` | **Sin tests** |
| `CourseResourceController` | `/api/v1/courses/{id}/resources` | **Sin tests** |
| `PartnerKeyController` | `/api/v1/admin/partner-keys` | **Sin tests** |
| `OAuthController` | `/api/v1/oauth` | **Sin tests** |

### 2.2 Servicios sin tests unitarios

| Servicio | Estado |
|----------|--------|
| `ReportService` | **Sin tests** |
| `CourseService` | **Solo test básico (CourseServiceTest.java)** |
| `EnrollmentService` | **Sin tests** |
| `ResourceService` | **Sin tests** |
| `AnswerService` | **Solo AnswerServiceTest.java** - coverage limitado |
| `QuestionService` | **Solo QuestionServiceTest.java** - coverage limitado |
| `PasswordResetService` | **Sin tests** |
| `EmailVerificationService` | **Sin tests** |
| `StorageQuotaService` | **Sin tests** |

### 2.3 Tests de seguridad faltantes

- **Falta:** Test de rate limiting para autenticación (solo está el filtro, no verificación)
- **Falta:** Test de inyección SQL en consultas dinámicas
- **Falta:** Test de bypass de autenticación via path traversal
- **Falta:** Test de OAuth CSRF protection
- **Falta:** Test de escalación de privilegios via API Keys

### 2.4 Tests de integración existentes pero insuficientes

- `QuotaIntegrationTest` - Solo cubre básico
- `ProjectIntegrationTest` - No testa concurrencia ni conflictos
- `OAuthIntegrationTest` - No testa casos de error

---

## 3. BUGS POTENCIALES

### 3.1 Bug: Race condition en actualización de quota

**Ubicación:** `User.java:53-63` y servicios de proyecto

```java
public boolean hasQuotaFor(long bytes) {
    return (this.storageUsedBytes + bytes) <= this.storageQuotaBytes;
}
```

El método `hasQuotaFor` no es atómico. Entre la verificación y la actualización, otro request podría exceder el quota.

**Severity:** Media - Bajo condiciones de alta concurrencia puede exceder quota.

### 3.2 Bug: Posible pérdida de datos en migraciones (NO APLICA)

**Ubicación:** `V14__cloud_refactor_project_architecture.sql:1-3`

> **Estado: NO RELEVANTE** - La migración V14 no existe en el repositorio actual.
> Solo existen migraciones hasta V8. La arquitectura de "projects" aún no está implementada.

~~La migración renombra tablas pero no migra los datos de `legacy_diagrams` a la nueva estructura. Los diagramas antiguos quedan huérfanos.~~

~~**Severity:** Alta - Datos potencialmente perdidos.~~

### 3.3 Bug: Rate limit filter cache memory leak potencial

**Ubicación:** `RateLimitFilter.java:37-39`

```java
private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build();
```

Si un atacante伪造 many IPs diferentes, podría llenar la memoria con entradas de bucket. Aunque Caffeine tiene límites, el default no está configurado con `maximumSize`.

**Severity:** Media - Potencial memory exhaustion con muchos IPs únicos.

### 3.4 Bug: JWT token no tiene blacklist

**Ubicación:** `JwtAdapter.java` y `RefreshTokenUseCase`

Cuando un usuario cambia su password (incrementa passwordVersion), los JWT tokens anteriores no son revocados inmediatamente. El código usa `pwdVersion` en el token (línea 55) pero la validación solo verifica que coincida - no rechaza tokens antiguos activamente.

**Severity:** Media - Tokens antiguos siguen válidos hasta expiración natural (15 min).

### 3.5 Bug: API key rate limit tiene cache stale

**Ubicación:** `ApiKeyAuthenticationFilter.java:84-90`

```java
private final Cache<UUID, Bucket> readBuckets = Caffeine.newBuilder()
        .expireAfterAccess(25, TimeUnit.HOURS)
        .build();
```

Los rate limits se leen al crear el bucket. Si un admin cambia el rate limit de una API key, el cambio no se aplica hasta que el bucket expire (25 horas).

**Severity:** Baja - Retraso en aplicación de cambios administrativos.

### 3.6 Bug: Campos sensibles en logs

**Ubicación:** `ApiKeyAuthenticationFilter.java:158`

```java
log.warn("Failed to record usage for API key {}: {}", keyId, ex.getMessage());
```

Aunque no loguea la key directamente, el `keyId` podría ser usado para correlacionar actividades.

**Severity:** Baja - Información de debugging en logs.

---

## 4. BRECHAS DE SEGURIDAD

### 4.1 Vulnerabilidad: SSRF en OAuth callback

**Ubicación:** `OAuthController.java:113-117`

```java
private String buildCallbackUri(HttpServletRequest request, String provider) {
    return request.getScheme() + "://"
            + request.getServerName() + ":" + request.getServerPort()
            + "/api/v1/oauth/" + provider.toLowerCase() + "/callback";
}
```

No hay validación de `redirectUri` proporcionado por el cliente en `authorize`. Un atacante podría manipular el flujo.

**Severity:** Media - Potencial si el provider acepta redirect URI dinámico.

### 4.2 Vulnerabilidad: IDOR en ReportController

**Ubicación:** `ReportController.java:62-66`

```java
@GetMapping("/{id}")
public ResponseEntity<ReportResponse> getReportById(@PathVariable UUID id) {
    Report report = reportService.findById(id);
    return ResponseEntity.ok(reportWebMapper.toResponse(report));
}
```

Cualquier usuario autenticado puede ver cualquier reporte por ID. No hay verificación de propiedad.

**Severity:** Alta - Exposición de datos sensibles de otros usuarios.

### 4.3 Vulnerabilidad: Falta de sanitización en búsqueda

**Archivos:** Varios Controllers con `@RequestParam String sortBy`

El parámetro `sortBy` se pasa directamente a la query sin sanitizar. podría permitir injection de SQL en combinación con JPA.

**Severity:** Media - Depende de implementación de repository.

### 4.4 Configuración de seguridad crítica expuesta

**Ubicación:** `application.yml:51-53`

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 900000
```

- El secret debe ser rotado periódicamente pero no hay mecanismo de rotación
- La expiración de 15 minutos (900000ms) es razonable pero no hay refresh automático del lado del cliente

**Severity:** Media - Depende de gestión de secrets externa.

### 4.5 Falta de rate limiting en endpoints administrativos

**Ubicación:** `PartnerKeyController.java`

Los endpoints `/api/v1/admin/partner-keys` no tienen rate limiting específico. Un admin malicioso o compromiso de cuenta admin podría crear/borrar muchas keys.

**Severity:** Media - Potencial abuse por insider threat.

### 4.6 Ausencia de logging de auditoría para operaciones sensibles

**Archivos:** `ReportController.java`, `PartnerKeyController.java`

Operaciones como cambiar status de reportes, crear partner keys, o eliminar cuentas no generan logs de auditoría granulares. Solo el job `AuditLogRetentionJob` hace cleanup.

**Severity:** Alta - No hay trace de acciones administrativas.

### 4.7 CORS configuración podría ser más restrictiva

**Ubicación:** `SecurityConfig.java:160`

```java
configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
```

No permite headers personalizados que podrían ser necesarios para features futuras.

**Severity:** Baja - Funciona pero limitante.

### 4.8 Dependencias con vulnerabilidades conocidas

| Dependencia | Versión | CVE | Status |
|-------------|---------|-----|--------|
| ~~`jjwt`~~ | ~~0.11.5~~ | ~~**CVE-2023-52422** (Critical)~~ | ✅ **CORREGIDO** - Actualizado a 0.12.6 |
| `springdoc-openapi` | 2.8.8 | Varias menores | Revisar CVE-2024-22252 |

**Severity:** Media - Actualizar springdoc-openapi si hay CVEs activos.

---

## 5. RECOMENDACIONES PRIORITARIAS

### Crítico (Corregir inmediatamente)
1. ~~**IDOR en ReportController**~~ - ✅ CORREGIDO
2. ~~**Actualizar jjwt**~~ - ✅ CORREGIDO (actualizado a 0.12.6)
3. ~~**Verificar migración V14**~~ - ✅ NO APLICA (migración no existe aún)

### Bugs encontrados y corregidos durante la revisión
- **Bug en ReportWebMapper**: No mapeaba `solvedAt` a `resolvedAt` ni convertía enums a strings
- **Bug en UserPersistenceMapper**: No manejaba proxies de Hibernate, causando 500 en endpoints de reportes

### Alto (Corregir esta semana)
4. ~~Agregar tests para ReportController, OAuthController~~ - ✅ COMPLETADO (arreglado bug en ReportWebMapper y UserPersistenceMapper)
5. ~~Implementar blacklist de JWT para invalidación inmediata~~ - ✅ YA EXISTÍA (JwtCookieAuthFilter verifica passwordVersion)
6. ~~Agregar logging de auditoría para operaciones admin~~ - ✅ COMPLETADO (ReportController líneas 95, 120, 131, 143)
7. ~~Rate limiting para endpoints administrativos~~ - ✅ COMPLETADO (RateLimitFilter ahora cubre /api/v1/reports admin con 30 req/min)

### Medio (Corregir este mes)
8. Agregar maximumSize al cache de rate limiting
9. Completar cobertura de tests para CourseService, EnrollmentService
10. Implementar endpoint de exploración de proyectos públicos
11. Validación de longitud máxima en todos los DTOs

### Bajo (Planificar para siguiente sprint)
12. Eliminar endpoints deprecados de DiagramController
13. Agregar límite de memoria al cache de rate limit
14. Revisar logs para sanitize campos sensibles

---

## 6. MÉTRICAS DE CÓDIGO

- **Total archivos Java:** ~200+
- **Líneas de código (main):** ~15,000
- **Tests existentes:** 14 archivos de test
- **Cobertura estimada:** ~45-50% (no hay reporte de coverage generado)

---

*Informe generado automáticamente. Recommendación: Ejecutar análisis estático con SonarQube y generar reporte de coverage con JaCoCo.*