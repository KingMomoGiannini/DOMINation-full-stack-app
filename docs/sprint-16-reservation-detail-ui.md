# Sprint 16 - Reservation detail UI

## Objetivo

Mejorar la lectura operativa de cada reserva sin sumar una vista de detalle separada ni romper la escaneabilidad de los listados.

## Patron introducido

Se consolido una tarjeta reutilizable de detalle (`ReservationDetailCard`) para user y provider.

La tarjeta prioriza cuatro capas:

1. Que es la reserva
2. Cuando ocurre
3. En que estado operativo y persistido esta
4. Que acciones o restricciones aplican

Los metadatos secundarios, como la referencia interna y la fecha de creacion, quedan al final y con menor peso visual.

## Representacion de lifecycle en UI

Cada tarjeta muestra dos badges:

- estado operativo derivado (`UPCOMING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- estado persistido (`PENDING`, `CONFIRMED`, `CANCELLED`)

Ademas:

- el bloque de agenda muestra dia, rango horario y duracion
- la tarjeta resalta visualmente una reserva `IN_PROGRESS`
- el texto contextual usa la semantica ya definida en Sprint 15

## Cancelabilidad

La UI usa directamente:

- `cancellable`
- `cancellationBlockReason`

Esto evita heuristicas duplicadas en componentes.

La accion de cancelacion:

- aparece como CTA cuando la reserva todavia puede cancelarse
- pasa a mostrarse como restriccion explicita cuando ya no corresponde
- mantiene feedback mas claro para usuario

## Diferencias por audiencia

### Usuario

Prioriza:

- lectura de la franja
- que incluye la reserva
- si sigue cancelable o no
- confianza sobre el estado actual

### Provider

Prioriza:

- sucursal
- cliente o fallback de identificacion
- contexto temporal
- estado operativo para accion diaria

## Pendientes

- Si en el futuro aparece una vista dedicada de detalle, conviene reutilizar la misma jerarquia visual en lugar de abrir otra semantica.
- Si el backend incorpora mas motivos de bloqueo de cancelacion, la UI puede extender el copy sin rehacer la tarjeta.
