# Sprint 21 - Historial operativo de reservas en UI

## Objetivo

Mostrar en el frontend el historial operativo persistido por `booking-service`, tanto para clientes como para providers, sin cargar auditorías innecesariamente en los listados de reservas.

El sprint reutiliza el contrato incorporado en Sprint 20 y no modifica backend, reglas de autorización ni persistencia.

## Endpoint consumido

```http
GET /api/booking/reservations/{id}/audit
Authorization: Bearer <token>
```

La consulta usa el cliente HTTP compartido del frontend. El interceptor existente incorpora el JWT y mantiene el manejo habitual de sesión expirada.

La respuesta se tipa como una lista de `ReservationAuditEvent`, ordenada por `createdAt` ascendente desde el backend.

## Archivos creados/modificados

### Creados

- `frontend/src/components/reservations/ReservationAuditTimeline.tsx`
- `docs/sprint-21-reservation-audit-ui.md`

### Modificados

- `frontend/src/types/booking.ts`
- `frontend/src/api/booking.api.ts`
- `frontend/src/components/reservations/ReservationDetailCard.tsx`
- `frontend/src/features/booking/pages/MyReservationsPage.tsx`
- `frontend/src/features/provider/pages/ProviderReservationsPage.tsx`
- `frontend/src/App.css`

## Tipos y cliente API

Se agregaron:

- `ReservationAuditEventType`
- `ReservationAuditEvent`
- `getReservationAudit(reservationId: number)`

Los tipos reflejan el DTO real de Sprint 20:

- `id`
- `reservationId`
- `actorUserId`
- `actorRole`
- `eventType`
- `reason`
- `comment`
- `createdAt`

No se persiste auditoría en `localStorage` ni se incorpora un estado global adicional.

## Componente nuevo

`ReservationAuditTimeline` concentra:

- apertura y cierre del panel
- consulta lazy por reserva
- traducción de tipos de evento
- presentación de fecha y hora en formato `es-AR`
- presentación sobria de rol e ID interno del actor
- visualización de motivo y comentario cuando existen
- estados de carga, error, reintento y lista vacía

Textos de evento:

- `CREATED` → `Reserva creada`
- `CANCELLED_BY_CUSTOMER` → `Cancelada por cliente`
- `CHECKED_IN` → `Check-in registrado`
- `MARKED_NO_SHOW` → `No-show registrado`

Los motivos técnicos conocidos también se muestran con texto legible. Si aparece un motivo nuevo, el componente conserva su valor original para no ocultar información.

## Flujo de carga lazy

1. `ReservationDetailCard` renderiza el control `Ver historial` sin consultar el endpoint.
2. Al abrir el panel, React Query habilita la consulta con la clave `['reservationAudit', reservationId]`.
3. Al cerrar, el contenido deja de mostrarse y la respuesta queda en la caché en memoria de React Query.
4. Al volver a abrir, React Query puede reutilizar la respuesta y actualizarla según su política de datos vencidos.
5. Después de una cancelación, check-in o no-show exitosos, se invalida únicamente la auditoría de la reserva afectada.

Esta decisión evita multiplicar requests al cargar listados paginados o con muchas cards.

## Integración en UI

El historial se integra una sola vez en `ReservationDetailCard`, componente compartido por:

- `MyReservationsPage`
- `ProviderReservationsPage`

Las páginas no duplican la lógica de carga ni de presentación. Sólo invalidan la consulta puntual después de una acción que genera un nuevo evento operativo.

El panel mantiene el lenguaje visual actual de reservas y adapta encabezados, errores y fechas a pantallas angostas.

## Estados UI

- Cerrado: muestra el CTA `Ver historial` y no realiza requests.
- Loading: muestra `Cargando historial…` dentro del panel.
- Success: muestra la línea temporal en el orden recibido.
- Empty: muestra `No hay eventos registrados.`.
- Error: muestra un mensaje legible mediante `getApiErrorMessage` y el botón `Reintentar`.

Un error de auditoría queda contenido dentro del panel y no bloquea ni reemplaza la información principal de la reserva.

## Decisiones de UX

- Se eligió un panel desplegable por card para preservar la escalabilidad de los listados.
- Se reutilizó la card compartida para mantener el mismo comportamiento en cliente y provider.
- No se intentan resolver usernames: se muestra el rol traducido y el `actorUserId` como referencia interna.
- `reason` y `comment` son sólo de lectura.
- El botón expone `aria-expanded` y `aria-controls`; los estados de carga y error informan su estado a tecnologías asistivas.

## Tests y validación técnica

El frontend no tiene actualmente infraestructura de tests automatizados estable: `package.json` no define un script de test ni incluye un runner o librería de testing. Por ese motivo no se agregaron tests automatizados ni dependencias nuevas en este sprint.

Validaciones ejecutadas:

```bash
cd frontend
npm run build
```

Resultado: build exitoso con validación de TypeScript y bundle de producción de Vite.

El script `npm run lint` existe, pero actualmente no puede ejecutarse porque el frontend no contiene un archivo de configuración de ESLint. Esta limitación es preexistente y queda pendiente de regularización.

## Cómo probar manualmente

### Preparación

1. Levantar la infraestructura y los servicios desde `infra/`.
2. Iniciar el frontend con `npm run dev`.
3. Contar con una reserva propia y una reserva vinculada a una sucursal del provider.
4. Generar eventos mediante creación, cancelación, check-in o no-show según corresponda.

### Casos mínimos

1. Ingresar como `ROLE_USER`, abrir `Mis reservas` y seleccionar `Ver historial` en una reserva propia.
2. Verificar que los eventos aparezcan con descripción, fecha, rol, actor y datos opcionales disponibles.
3. Ingresar como `ROLE_PROVIDER`, abrir `Reservas` y consultar una reserva asociada a una de sus sucursales.
4. Intentar consultar sin ownership, o simular una respuesta `404/403`, y verificar que la card permanezca visible con error y opción de reintento dentro del panel.
5. Probar una respuesta vacía y verificar `No hay eventos registrados.`.
6. Detener temporalmente el gateway o simular un error de red y verificar que no se rompa la card principal.
7. Abrir y cerrar el panel varias veces y confirmar que no altera filtros, paginación ni acciones de la reserva.
8. Con el panel abierto, cancelar o registrar check-in/no-show y verificar que el historial incorpore el nuevo evento después de la actualización.

## Pendientes

- Incorporar Vitest o la estrategia de testing frontend que se defina para el proyecto.
- Agregar tests del componente para loading, error, empty, retry y traducción de eventos.
- Regularizar la configuración de ESLint para que el script existente sea ejecutable.
- Evaluar paginación o resumen de eventos si el historial crece de forma significativa.
- Enriquecer el actor con un nombre legible sólo si aparece un contrato backend seguro y estable.
- Mantener fuera de alcance la edición de comentarios o motivos hasta definir reglas operativas y autorización específicas.

## Resumen técnico

Sprint 21 vuelve visible la auditoría operativa de Sprint 20 mediante un componente compartido, tipado y de carga lazy. La solución conserva los contratos y la seguridad existentes, evita requests masivos en listados, mantiene aislados los errores del historial y refresca la auditoría después de acciones operativas exitosas.
