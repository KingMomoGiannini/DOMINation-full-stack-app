# Sprint 10 — Plataforma booking/auth/enriquecimiento (DOMINation V2)

## 1. Archivos creados/modificados

### Booking-service
- `src/main/resources/db/migration/V202604101200__booking_core_schema_baseline.sql` — baseline idempotente (`CREATE TABLE IF NOT EXISTS` + índices).
- `src/main/resources/application-dev.properties` — `ddl-auto=update` + Flyway.
- `src/main/resources/application-prod.properties` — `ddl-auto=validate` + Flyway (reemplaza el comentario “cuando exista baseline”).
- `src/main/resources/application.properties` — comentarios de perfiles Flyway/JPA.
- `observability/ReservationEnrichmentMetrics.java` — contadores Micrometer.
- `service/CatalogClient.java` — logs `[enrichment]` con `outcome` explícito + métricas por resultado.
- `service/AuthClient.java` — idem + métrica `skipped_no_authorization` si se llama sin header.
- `service/ReservationDtoEnricher.java` — logs de fase alineados al prefijo `[enrichment]`.
- `config/RestClientConfig.java` — `trace` al propagar `X-Request-Id` en salidas HTTP.
- `pom.xml` — `wiremock-standalone` (test).
- `src/test/java/.../integration/FlywayBookingSchemaIT.java` — Flyway + PostgreSQL Testcontainers + JDBC.
- `src/test/java/.../integration/ReservationEnrichmentWireMockIT.java` — contrato HTTP catálogo/auth + `X-Request-Id`.

### Auth-service
- `controller/UserController.java` — JavaDoc del riesgo residual de enumeración y mitigaciones sugeridas (sin cambiar contrato).

### Documentación
- Este archivo; referencias cruzadas en `application.properties`.

---

## 2. Baseline o estrategia Flyway/JPA

| Orden Flyway | Script | Rol |
|---------------|--------|-----|
| 1 | `V202604051200__add_reservation_display_snapshots.sql` | En BD existente sin tablas: sin-op; si ya hay tablas: asegura columnas snapshot. |
| 2 | `V202604101200__booking_core_schema_baseline.sql` | Crea `reservations` / `reservation_lines` con **todas** las columnas del modelo actual (incl. snapshot) si aún no existen. |

**Convivencia JPA:**

| Perfil / uso | `ddl-auto` | Flyway | Intención |
|--------------|------------|--------|-----------|
| Sin perfil (default `application.properties`) | `update` | `true` | Desarrollo rápido sin tocar perfiles (compatible con Sprints anteriores). |
| `dev` | `update` | `true` | Explícito para local: Hibernate puede ajustar; migraciones siguen versionadas. |
| `prod` | `validate` | `true` | Esquema = Flyway; Hibernate solo valida el mapping. |

**Decisión:** no se introdujo `V1__` anterior a `202604051200` para no generar **out-of-order** en bases que ya aplicaron la migración Sprint 9; el baseline “completo” va en versión **posterior** (`202604101200`) para mantener orden monótono.

---

## 3. Qué reforzó cada cambio

- **Baseline SQL:** despliegues nuevos pueden depender de Flyway para tablas núcleo; menos ambigüedad con “solo Hibernate”.
- **`validate` en prod:** fallo temprano si entidad y BD divergen.
- **Métricas `booking.enrichment.*`:** series temporales por `resource` + `outcome` (Prometheus ya expuesto en Actuator).
- **Logs unificados `[enrichment]`:** filtrado y alertas más simples; `hint=` donde aplica (legacy vs token).
- **Trace de `X-Request-Id`:** visibilidad en logs TRACE del cliente HTTP hacia auth/catalog.
- **JavaDoc auth:** decisión explícita sobre enumeración sin cambiar API.

---

## 4. Tests de integración/contrato agregados

| Test | Tecnología | Qué valida |
|------|------------|------------|
| `FlywayBookingSchemaIT` | Testcontainers PostgreSQL + API Flyway + JDBC metadata | Migraciones aplican; existen tablas y columnas `branch_name` / `item_name`. |
| `ReservationEnrichmentWireMockIT` | Spring context mínimo + WireMock | Éxito/404 catálogo y métricas; enriquecimiento de listado vía catálogo; llamada auth `handle-for-provider`; propagación de `X-Request-Id`. |

**No incluido:** E2E con JWT real y gateway (coste/alto acoplamiento); se mantiene como deuda documentada si se desea más adelante.

---

## 5. Mejoras de observabilidad

- **Contadores Micrometer**
  - `booking.enrichment.catalog` — tags `resource` ∈ {`branch`,`item`}, `outcome` ∈ {`success`,`empty_payload`,`http_404`,`http_4xx`,`http_5xx`,`timeout`,`unexpected`}.
  - `booking.enrichment.auth` — tags `resource=handle`, `outcome` ∈ {`success`,`skipped_no_authorization`,`empty_payload`,`http_401_403`,`http_404`,`http_4xx`,`http_5xx`,`timeout`,`unexpected`}.
- **Logs:** prefijo `[enrichment]`, campos `dependency=`, `resource=`, `outcome=`, `requestId=`, `hint=` cuando ayuda a distinguir legacy vs fallo de credenciales.

---

## 6. Revisión del endpoint `handle-for-provider`

- **Diseño actual:** sigue siendo adecuado para el caso de uso (prestador enriquece listados con username público).
- **Exposición por `userId`:** necesaria mientras el booking identifique clientes por id numérico en `customerId`.
- **Enumeración:** riesgo residual aceptado y documentado en JavaDoc del controlador; mitigaciones posibles (rate limit, auditoría en gateway) sin cambiar el contrato en este sprint.
- **Seguridad:** sin cambios: sigue `ROLE_PROVIDER` + `@PreAuthorize` + `userId` positivo.

---

## 7. Cómo probarlo

1. `mvn test` en `services/booking-service` (requiere Docker para Testcontainers en `FlywayBookingSchemaIT`).
2. Arrancar booking con `--spring.profiles.active=prod` contra Postgres vacío: Flyway crea esquema; arranque con `validate` OK.
3. Local: `--spring.profiles.active=dev` o sin perfil (sigue `update` por defecto).
4. Métricas: `GET /actuator/prometheus` y buscar `booking_enrichment_*` (nombres con `_` según convención Prometheus).
5. Logs TRACE: activar `logging.level.com.domination.booking.config.RestClientConfig=TRACE` para ver propagación de `X-Request-Id`.

---

## 8. Resumen técnico del sprint

Se cerró la deuda de **baseline Flyway** sin romper el orden de migraciones ya desplegadas, se **formalizó prod** con `ddl-auto=validate`, y se añadió **observabilidad homogénea** (métricas + logs estructurados) alrededor del enriquecimiento tolerante. Los tests nuevos cubren **esquema real** (PostgreSQL + Flyway) y **contratos HTTP** hacia catálogo y auth (WireMock), incluyendo **correlación** `X-Request-Id`. El endpoint `handle-for-provider` se **revisó y documentó** en código sin ampliar superficie de ataque ni alterar rutas existentes.
