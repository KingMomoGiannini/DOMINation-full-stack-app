# Sprint 17: politicas operativas minimas para reservas

## Decisiones del sprint

Entraron dos politicas operativas con valor real inmediato para provider:

- `check-in`: representa un hecho humano real, util para dejar asentado que el cliente se presento durante la franja.
- `no-show`: representa un hecho humano real, util para dejar asentado que la franja termino y el cliente no se presento.

No entro `cierre manual/completado operacional` en este sprint porque hoy el sistema ya expresa bien la finalizacion temporal con `operationalStatus=COMPLETED`. Persistir un cierre manual ahora duplicaria semantica sin cambiar reglas de negocio ni habilitar una accion realmente distinta.

## Que se persiste

Se persisten solo hechos operativos humanos:

- `checkedInAt`
- `noShowMarkedAt`

No se persisten estados temporales derivados por reloj. Sigue siendo derivado:

- `operationalStatus` (`UPCOMING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- `cancellable`
- motivos de bloqueo de cancelacion
- flags de disponibilidad de acciones provider y sus motivos de bloqueo
- `attendanceStatus`, que es una vista derivada a partir de hechos persistidos

## Reglas operativas

### Check-in

- Lo puede ejecutar solo `ROLE_PROVIDER` sobre reservas de sus propias sucursales.
- Solo aplica si la reserva no esta cancelada.
- Solo aplica desde `startAt` inclusive hasta antes de `endAt`.
- Queda bloqueado si ya existe `checkedInAt`.
- Queda bloqueado si la reserva ya fue marcada como `no-show`.

### No-show

- Lo puede ejecutar solo `ROLE_PROVIDER` sobre reservas de sus propias sucursales.
- Solo aplica si la reserva no esta cancelada.
- Solo aplica cuando la franja ya termino (`now >= endAt`).
- Queda bloqueado si ya existe `checkedInAt`.
- Queda bloqueado si ya existe `noShowMarkedAt`.

## Interaccion con lifecycle y cancelacion

- `status` persistido principal no cambia: la reserva sigue siendo `CONFIRMED` o `CANCELLED`.
- `operationalStatus` sigue describiendo tiempo y cancelacion, no asistencia.
- Una reserva cancelada pasa a `attendanceStatus=NOT_APPLICABLE`.
- La politica de cancelacion del usuario no cambia: sigue disponible solo antes del inicio.
- `check-in` y `no-show` no reabren ni alteran cancelacion; agregan trazabilidad operativa.

## Contratos y UI

Se agregaron al contrato de reserva:

- timestamps persistidos: `checkedInAt`, `noShowMarkedAt`
- vista derivada de asistencia: `attendanceStatus`
- flags y motivos para CTA provider:
  - `providerCheckInAllowed`
  - `providerCheckInBlockReason`
  - `providerMarkNoShowAllowed`
  - `providerMarkNoShowBlockReason`

El listado provider agrega filtro por operacion:

- `ALL`
- `NOT_RECORDED`
- `CHECKED_IN`
- `NO_SHOW`
- `NOT_APPLICABLE`

Las metricas provider agregan:

- `checkedIn`
- `noShow`

## Pendiente

Queda pendiente para futuros sprints:

- una politica de gracia previa al check-in
- cierre manual si aparece una necesidad operacional concreta distinta de la finalizacion temporal
- auditoria mas rica (`performedBy`, comentarios, motivo libre)
- acciones admin sobre incidentes de asistencia si el negocio las necesita
