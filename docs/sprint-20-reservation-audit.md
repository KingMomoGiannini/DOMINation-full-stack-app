# Sprint 20 - Auditoria operativa minima de reservas

## Objetivo

Agregar trazabilidad explicita para acciones importantes del modulo de reservas sin sobrediseniar el dominio ni reemplazar el lifecycle existente.

El sprint registra eventos auditables para:

- creacion de reserva
- cancelacion por cliente
- check-in realizado por provider
- no-show marcado por provider

## Contexto

En Sprint 17 se incorporaron hechos operativos humanos sobre reservas:

- `checkedInAt`
- `noShowMarkedAt`

Esos campos dicen que un hecho ocurrio, pero no dejan una linea historica consultable ni registran actor, rol o motivo. Sprint 20 agrega esa primera capa de auditoria operacional.

## Archivos creados/modificados

### Creados

- `services/booking-service/src/main/java/com/domination/booking/domain/ReservationAuditEvent.java`
- `services/booking-service/src/main/java/com/domination/booking/domain/ReservationAuditEventType.java`
- `services/booking-service/src/main/java/com/domination/booking/dto/ReservationAuditEventDTO.java`
- `services/booking-service/src/main/java/com/domination/booking/mapper/ReservationAuditEventMapper.java`
- `services/booking-service/src/main/java/com/domination/booking/repository/ReservationAuditEventRepository.java`
- `services/booking-service/src/main/java/com/domination/booking/service/ReservationAuditService.java`
- `services/booking-service/src/main/resources/db/migration/V202606241200__add_reservation_audit_events.sql`
- `services/booking-service/src/test/java/com/domination/booking/service/ReservationAuditServiceTest.java`
- `docs/sprint-20-reservation-audit.md`

### Modificados

- `services/booking-service/src/main/java/com/domination/booking/controller/ReservationController.java`
- `services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`
- `services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceTest.java`
- `services/booking-service/src/test/java/com/domination/booking/integration/FlywayBookingSchemaIT.java`

## Entidad nueva

Se agrego `ReservationAuditEvent`.

Campos:

- `id`
- `reservation`
- `actorUserId`
- `actorRole`
- `eventType`
- `reason`
- `comment`
- `createdAt`

La entidad referencia a `Reservation` por `reservation_id`. No agrega estados nuevos a la reserva ni cambia el significado de `status`, `checkedInAt` o `noShowMarkedAt`.

## Enum nuevo

Se agrego `ReservationAuditEventType`.

Valores iniciales:

- `CREATED`
- `CANCELLED_BY_CUSTOMER`
- `CHECKED_IN`
- `MARKED_NO_SHOW`

El enum queda acotado a hechos reales ya implementados en el producto.

## Endpoint nuevo

Se agrego:

- `GET /api/booking/reservations/{id}/audit`

Devuelve una lista de `ReservationAuditEventDTO` ordenada por `createdAt` ascendente.

El DTO expone:

- `id`
- `reservationId`
- `actorUserId`
- `actorRole`
- `eventType`
- `reason`
- `comment`
- `createdAt`

No expone la entidad `Reservation` completa ni detalles internos innecesarios.

## Reglas de autorizacion

El endpoint requiere JWT y acepta:

- `ROLE_USER`
- `ROLE_PROVIDER`
- `ROLE_ADMIN`

Reglas:

- `ROLE_USER` puede ver auditoria solo de reservas donde `customerId` coincide con su `userId`.
- `ROLE_PROVIDER` puede ver auditoria solo de reservas donde `providerId` coincide con su `userId`.
- `ROLE_ADMIN` puede ver auditoria de cualquier reserva.

Si el usuario autenticado no tiene ownership sobre la reserva, el servicio responde como reserva no encontrada para no filtrar existencia.

## Registro de eventos

La auditoria se registra desde `ReservationService`, despues de transiciones exitosas:

- `createReservation` registra `CREATED`.
- `cancelReservation` registra `CANCELLED_BY_CUSTOMER`.
- `providerCheckInReservation` registra `CHECKED_IN`.
- `providerMarkNoShow` registra `MARKED_NO_SHOW`.

No se registran eventos en intentos fallidos.

La cancelacion idempotente no genera eventos duplicados cuando la reserva ya estaba cancelada.

## Migracion

Se agrego la migracion:

- `V202606241200__add_reservation_audit_events.sql`

Crea la tabla:

- `reservation_audit_events`

Indices minimos:

- `idx_reservation_audit_events_reservation_id`
- `idx_reservation_audit_events_reservation_created_at`

La tabla usa foreign key hacia `reservations(id)` con `ON DELETE CASCADE`, manteniendo la auditoria dentro del limite del agregado de reserva.

## Tests

Se agrego cobertura para:

- registrar evento al crear reserva
- registrar evento al cancelar reserva
- registrar evento al hacer check-in
- registrar evento al marcar no-show
- no registrar evento en cancelacion idempotente
- no registrar evento en acciones fallidas
- permitir lectura de auditoria al usuario duenio de la reserva
- permitir lectura de auditoria al provider duenio de la reserva
- permitir lectura de auditoria a admin
- rechazar lectura cuando no hay ownership

Tambien se actualizo `FlywayBookingSchemaIT` para validar la tabla e indices de auditoria cuando Testcontainers tiene Docker disponible.

## Como probar

### Tests automatizados

Desde `services/booking-service`:

```bash
..\auth-service\mvnw.cmd -f pom.xml test
```

Para validar especificamente la migracion Flyway con Testcontainers:

```bash
..\auth-service\mvnw.cmd -f pom.xml -Dtest=FlywayBookingSchemaIT test
```

Si Docker no esta disponible, el test de Flyway se salta sin romper la suite.

### Prueba manual del endpoint

1. Crear una reserva autenticado como `ROLE_USER`.
2. Cancelar esa reserva como el mismo usuario, o ejecutar check-in/no-show como provider duenio.
3. Consultar:

```http
GET /api/booking/reservations/{id}/audit
Authorization: Bearer <token>
```

4. Verificar que un usuario que no es duenio no pueda ver la auditoria.
5. Verificar que un provider solo vea auditoria de reservas asociadas a su `providerId`.

## Pendientes

Queda pendiente para futuros sprints:

- comentarios libres enviados desde UI
- motivo libre cargado por provider/admin
- auditoria de acciones admin si se incorporan operaciones sobre incidentes
- filtros o paginacion de auditoria si el volumen crece
- mostrar auditoria en UI de detalle/provider
- enriquecer actor con username cuando haga falta, sin duplicar datos sensibles

## Resumen tecnico

Sprint 20 agrega una auditoria operativa minima, persistente y consultable para reservas. La solucion mantiene la arquitectura en capas, no cambia contratos existentes, no altera lifecycle ni cancelacion, y deja preparado el camino para trazabilidad mas rica sin introducir complejidad prematura.
