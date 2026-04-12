# Sprint 15 - Reservation lifecycle

## Objetivo

Clarificar el ciclo de vida operativo de las reservas sin introducir una maquina de estados persistida innecesaria.

## Estados persistidos

Los estados guardados en base siguen siendo acotados y representan eventos de negocio explicitamente confirmados:

- `PENDING`: reservado para flujos futuros donde una reserva todavia no quedo confirmada operativamente.
- `CONFIRMED`: reserva activa creada correctamente y vigente mientras no se cancele.
- `CANCELLED`: reserva cancelada de forma idempotente.

En el flujo actual de DOMINation V2, la creacion persiste directamente como `CONFIRMED` porque la reserva ya nace validada y operativa. Mantenerla en `PENDING` generaba ambiguedad entre backend y frontend.

## Estados derivados

La lectura operativa se deriva en tiempo de respuesta a partir de `status`, `startAt`, `endAt` y el reloj actual:

- `UPCOMING`: reserva no cancelada cuyo inicio es futuro.
- `IN_PROGRESS`: reserva no cancelada cuyo rango ya comenzo y aun no termino.
- `COMPLETED`: reserva no cancelada cuyo fin ya paso.
- `CANCELLED`: espejo operativo del estado persistido cancelado.

Estos estados no se persisten para evitar duplicar semantica temporal, jobs de sincronizacion o inconsistencias por paso del tiempo.

## Reglas de transicion

- Creacion:
  - `CONFIRMED`
- Cancelacion antes del inicio:
  - `CONFIRMED -> CANCELLED`
  - `PENDING -> CANCELLED`
- Paso del tiempo:
  - no cambia el estado persistido
  - si cambia el `operationalStatus`
- Cierre/completitud:
  - por ahora no persiste un `COMPLETED`
  - una reserva se considera completada cuando `endAt <= now` y no esta cancelada

## Regla de cancelacion

La regla vigente se mantiene: una reserva solo puede cancelarse antes de su hora de inicio.

Para volverla explicita en contrato y UI se agregan:

- `cancellable`
- `cancellationBlockReason`

Esto evita que el frontend tenga que deducir la regla comparando fechas o estados por su cuenta.

## Cambios de contrato

`ReservationDTO` expone ahora:

- `status`: estado persistido
- `operationalStatus`: estado operativo derivado
- `cancellable`: flag listo para UI
- `cancellationBlockReason`: motivo simple cuando no puede cancelarse

## Filtros y metricas

Provider reservations deja de usar la nocion ambigua de `PAST` y pasa a:

- `UPCOMING`
- `IN_PROGRESS`
- `COMPLETED`

Las metricas del provider tambien se alinean con esa semantica:

- `total`
- `cancelled`
- `upcoming`
- `inProgress`
- `completed`

`GET /api/booking/my/reservations` sigue siendo lista completa, pero ahora con semantica operativa explicita por item.

## Impacto en frontend

- User y provider muestran badge operativo y badge de estado persistido.
- La accion de cancelacion se pinta solo cuando la reserva es cancelable.
- El copy operativo distingue proximas, en curso, finalizadas y canceladas.

## Decisiones postergadas

- No se persiste `COMPLETED` todavia porque hoy solo reflejaria paso del tiempo.
- No se introduce una maquina de estados formal porque el dominio actual no la necesita.
- `PENDING` se conserva en el enum para no cerrar la puerta a flujos futuros de aprobacion o pago diferido.
