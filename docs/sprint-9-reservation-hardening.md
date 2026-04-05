# Sprint 9 — Endurecimiento post Sprint 8 (DOMINation V2)

## 1. Archivos creados/modificados

### Booking-service
- `pom.xml` — dependencias `flyway-core`, `flyway-database-postgresql`.
- `src/main/resources/db/migration/V202604051200__add_reservation_display_snapshots.sql` — migración idempotente.
- `src/main/resources/application.properties` — Flyway activo, timeouts `booking.http-client.*`.
- `src/main/resources/application-prod.properties` — Flyway en prod (Sprint 10: `ddl-auto=validate` con baseline `V202604101200`).
- `config/RestClientConfig.java` — timeouts configurables, propagación `X-Request-Id` desde MDC `requestId`.
- `service/CatalogClient.java` — `fetchBranchNameForEnrichment` / `fetchItemNameForEnrichment` (no lanzan; logs con `requestId` y distinción 404 vs error/timeout).
- `service/AuthClient.java` — manejo por código HTTP (401/403/404 vs otros), timeouts vía RestClient, logs con `requestId`.
- `service/ReservationDtoEnricher.java` — usa solo APIs tolerantes; salida temprana sin `Authorization`; caché por `branchId`/`itemId` en un mismo listado.
- `src/test/java/.../ReservationDtoEnricherTest.java` — **nuevo**.

### Auth-service
- `controller/UserController.java` — `@Validated`, `@Positive` en `userId` del handle provider.
- `exception/GlobalExceptionHandler.java` — `ConstraintViolationException` → 400 con `detail`.
- `pom.xml` — `h2` scope test.
- `src/test/resources/application-test.properties` — **nuevo**: H2 en memoria para tests.
- `src/test/java/.../AuthServiceApplicationTests.java` — `@ActiveProfiles("test")`.
- `src/test/java/.../controller/UserHandleForProviderMvcTest.java` — **nuevo**: seguridad y contrato del endpoint.

### Documentación
- `docs/sprint-8-reservation-contracts-enrichment.md` — sección “pendiente” alineada con Sprint 9.
- Este archivo.

---

## 2. Migraciones agregadas

| Script | Contenido |
|--------|-----------|
| `V202604051200__add_reservation_display_snapshots.sql` | `ALTER TABLE ... ADD COLUMN IF NOT EXISTS branch_name` / `item_name` dentro de bloque PL/pgSQL que solo corre si existe la tabla (`to_regclass`). |

**Comportamiento:** en base vacía el script no falla (sin tablas); en dev con `ddl-auto=update`, Hibernate puede crear tablas después de Flyway y las columnas ya están en el mapping JPA — el `IF NOT EXISTS` evita choque. En bases existentes sin columnas, Flyway las añade.

**Producción:** `application-prod.properties` habilita Flyway; **`spring.jpa.hibernate.ddl-auto=validate` queda comentado** hasta contar con una migración baseline del esquema completo de reservas (evita arranque roto).

---

## 3. Qué endureció cada cambio

- **Flyway**: contrato de esquema versionado para columnas Sprint 8 sin depender solo de Hibernate en entornos serios.
- **RestClient**: connect/read timeout evita hilos colgados ante catalog/auth caídos o lentos.
- **`X-Request-Id`**: correlación booking → dependencias (si el servicio downstream lo registra).
- **`CatalogClient` / `AuthClient`**: rutas de enriquecimiento devuelven `Optional` vacío y log estructurado en lugar de tumbar el listado.
- **`ReservationDtoEnricher`**: menos llamadas redundantes; sin header de prestador no invoca auth.
- **Auth**: `userId` positivo validado; respuesta 400 predecible; manejo global de violaciones.

---

## 4. Tests incorporados

| Ámbito | Qué cubre |
|--------|------------|
| `ReservationDtoEnricherTest` | Relleno desde catálogo (incl. dedupe branch/item), degradación si `Optional` vacío, auth omitido sin header, caché de username por `userId`. |
| `ReservationServiceTest` (existente) | Snapshot `branchName` / `itemName` al crear reserva. |
| `UserHandleForProviderMvcTest` | 200 con PROVIDER, 403 sin rol, 404 usuario inexistente, 400 `userId` ilegal; `@MockitoBean RoleRepository` para el `CommandLineRunner` del `AuthServiceApplication` en slice MVC. |
| `AuthServiceApplicationTests` | Contexto completo con perfil `test` (H2). |

**No añadido (criterio):** test con `MockRestServiceServer` sobre `RestClient` built: en Spring Framework 7 el bind es a `RestClient.Builder`; no se refactorizó el bean de producción solo para ese harness.

---

## 5. Comportamiento ante fallos de auth/catalog

- **Catálogo (enriquecimiento):** `fetchBranchName*` / `fetchItemName*` capturan `RestClientResponseException` (404 → log *info* “recurso ausente / sin snapshot”), otros 4xx/5xx → *warn*; `ResourceAccessException` → *warn* `timeout_or_network`. El DTO queda sin nombre; el listado **sigue 200**.
- **Auth (enriquecimiento provider):** sin `Authorization` → retorno inmediato sin llamadas; 401/403 → *warn* explícito sobre token/rol; 404 usuario → *info*; timeout → *warn*. `customerUsername` puede quedar null.
- **Flujo crítico crear reserva:** sigue usando `getBranchDetail` / `getItemDetail` que **sí propagan** error si el catálogo falla (comportamiento Sprint 5–8 intacto).

---

## 6. Observaciones de seguridad

- **Reenvío `Authorization`:** booking solo reenvía el Bearer del prestador al llamar a auth; no almacena ni loguea el token.
- **Exposición mínima:** el endpoint sigue devolviendo solo `userId` + `username`; sin email.
- **Autorización:** matcher `GET /users/*/handle-for-provider` → `ROLE_PROVIDER` + `@PreAuthorize`; cliente sin rol recibe 403.
- **Validación:** `userId` debe ser `@Positive` (evita valores triviales no válidos como identificadores).
- **Enumeración:** un prestador autenticado puede consultar handles por id numérico; asumido aceptable para operar reservas (mismo riesgo que listar `customerId` en DTOs).

---

## 7. Cómo probarlo

1. **Booking:** `mvn test` en `services/booking-service`.
2. **Auth:** `mvn test` en `services/auth-service` (perfil `test`, H2).
3. **Flyway:** levantar Postgres con esquema previo de reservas o vacío + `ddl-auto=update` en dev; verificar tabla `flyway_schema_history` y columnas `branch_name` / `item_name`.
4. **Manual:** cortar catalog o auth y llamar `GET .../my/reservations` y `GET .../provider/reservations` — debe responder 200 con campos opcionales vacíos y logs de degradación en booking.

---

## 8. Resumen técnico del sprint

Se formalizó el esquema incremental con **Flyway** y se endureció el **perímetro operativo** del enriquecimiento: timeouts, correlación opcional por **request-id**, y separación clara entre llamadas **estrictas** (crear reserva) y **tolerantes** (listados). El auth-service ganó validación de entrada y tests MVC bajo perfil **H2**, eliminando la dependencia de Postgres local para CI básico. La ruta a **`ddl-auto=validate` en producción** queda documentada como paso posterior a un **baseline** Flyway del esquema booking completo.
