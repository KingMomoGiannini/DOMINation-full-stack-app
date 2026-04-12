# Sprint 14 — Performance hardening de listados escalados

## Objetivo

Consolidar los listados escalados introducidos en Sprint 13 con soporte explícito de base de datos, consultas más defendibles y una decisión fundada sobre `GET /api/booking/my/reservations`.

## Cambios implementados

### Booking service

**Consultas y contratos**

- `GET /api/booking/provider/reservations`
  - mantiene paginación `Page`, filtros y contrato de Sprint 13.
  - ahora usa orden estable por `startAt` + `id` para evitar páginas ambiguas cuando varias reservas comparten la misma fecha de inicio.
  - valida `from <= to`; si la ventana es inválida devuelve `400 Bad Request` en lugar de procesar un rango contradictorio.
- `GET /api/booking/provider/reservations/metrics`
  - deja de ejecutar cuatro conteos separados.
  - pasa a una única query agregada por `providerId`, con `CASE WHEN` para total, canceladas, upcoming y past.
- `GET /api/booking/my/reservations`
  - **se mantiene como lista completa**.
  - se endurece el orden a `startAt desc, id desc` para tener historia consistente y aprovechar índice compuesto.

**Índices Flyway agregados**

Migración: `services/booking-service/src/main/resources/db/migration/V202604121700__optimize_reservation_listing_indexes.sql`

- `idx_reservations_customer_start_id (customer_id, start_at desc, id desc)`
  - cubre la lectura completa de `my reservations`, ordenada y sin sort extra.
- `idx_reservations_provider_start_id (provider_id, start_at desc, id desc)`
  - cubre el listado paginado del provider cuando no hay filtro por sucursal.
- `idx_reservations_provider_branch_start_id (provider_id, branch_id, start_at desc, id desc)`
  - cubre el listado paginado cuando el provider filtra por sucursal, un patrón explícito de la UI.

**Índices removidos**

- `idx_reservations_customer_id`
- `idx_reservations_provider_id`

Se eliminaron porque los nuevos índices compuestos ya cubren esos prefijos y evitan mantener índices redundantes.

### Auth service

**Consultas**

- `GET /admin/provider-requests`
  - mantiene `Page` de Sprint 13.
  - ahora usa orden estable por `createdAt` + `id` para que la paginación no dependa de timestamps empatados.
- `GET /admin/provider-requests/summary`
  - pasa de cuatro conteos separados a una sola agregación SQL generada por JPA.
- `GET /auth/provider-requests/me`
  - sigue devolviendo la última solicitud, pero con criterio estable `createdAt desc, id desc`.

**Soporte DB explícito**

`auth-service` todavía gestiona esquema con Hibernate (`ddl-auto=update`) y no tiene baseline Flyway propio. Para no introducir una migración aislada y riesgosa sin estrategia completa de esquema, se agregaron índices explícitos en el mapping JPA de `ProviderRequest`:

- `idx_provider_requests_created_at_id (created_at, id)`
  - cubre el listado admin por defecto, ordenado por fecha de alta.
- `idx_provider_requests_user_id_created_at_id (user_id, created_at, id)`
  - cubre `provider-requests/me` y el filtro admin por `userId`.
- `idx_provider_requests_status_created_at_id (status, created_at, id)`
  - cubre el listado admin por pestaña/estado y ayuda también a los summary por estado.

## Decisión sobre `GET /api/booking/my/reservations`

Se **mantiene como lista completa**.

### Motivo

- el caso de uso sigue siendo historial individual de un usuario, con volumen esperado mucho menor que el listado operativo del provider.
- paginarlo ahora agregaría complejidad de contrato y UI sin evidencia de presión equivalente a la del panel provider/admin.
- sí convenía endurecerlo en dos puntos sin cambiar contrato:
  - orden estable (`startAt desc, id desc`)
  - índice compuesto alineado con ese recorrido

En otras palabras: no se cambió por simetría, pero sí se protegió su camino de lectura actual.

## Validaciones y pruebas

### Ejecutadas

- `services/auth-service`: `ProviderRequestRepositoryTest`
  - valida índices explícitos del esquema generado.
  - valida query agregada de summary.
  - valida lectura “última solicitud” con desempate por `id`.
- `services/booking-service`: `ReservationServiceTest`
  - valida servicio actualizado para métricas y `my reservations`.
- `services/booking-service`: `ReservationListingRepositoryTest`
  - valida query agregada de métricas con JPA real + H2.
  - valida orden descendente estable en `my reservations`.

### No ejecutada en este entorno

- `FlywayBookingSchemaIT`
  - el test existe y fue actualizado para verificar los índices nuevos de booking.
  - no pudo completarse acá porque Testcontainers no encontró un entorno Docker válido.

## Impacto esperado

- menos trabajo de ordenamiento y menos lecturas amplias en listados paginados críticos.
- menor costo de summary/metrics al pasar de múltiples conteos a una sola agregación por endpoint.
- paginación más estable frente a timestamps empatados.
- protección explícita del caso `my reservations` sin abrir un cambio de contrato innecesario.

## Pendientes / riesgos

- `provider reservations` sigue usando offset pagination; con datasets mucho mayores, keyset/cursor sigue siendo la evolución natural.
- no se añadieron índices específicos por `status` o por ventana `endAt` en booking porque hoy no hay evidencia suficiente para justificar más costo de escritura; si el uso real muestra alta selectividad por esos filtros, conviene medir `EXPLAIN ANALYZE` en PostgreSQL real antes de agregar otro compuesto.
- `auth-service` todavía no tiene estrategia Flyway propia; cuando se formalice baseline/migraciones del servicio convendrá trasladar estos índices desde JPA a migraciones versionadas.
