# Sprint 6 — Estado: roles, provider/admin y UX (DOMINation V2)

Consolidación del frontend sobre la base del **Sprint 5**: guards por rol, páginas provider/admin alineadas a `features/*`, consumo **solo vía gateway**, y refino del flujo de reservas y UI compartida.

---

## Objetivos cumplidos

1. **Guards de autenticación y autorización** con componentes reutilizables.
2. **Rutas y navbar** según `ROLE_USER`, `ROLE_PROVIDER`, `ROLE_ADMIN`.
3. **Legacy movido** a `features/provider` y `features/admin` (eliminados `src/pages/Provider*.tsx` y `AdminProviderRequestsPage.tsx`).
4. **Solicitud de prestador** con TanStack Query, feedback de errores (incl. **409** del auth) y copy claro sobre **re-login** tras aprobación.
5. **Panel prestador** con datos reales: sucursales, edición, activar/desactivar (`PATCH`), eliminar con confirmación, salas (`ROOM`) por sucursal.
6. **Admin solicitudes** con filtros correctos (sin `?status=` vacío), aprobar/rechazar con **ConfirmDialog** y manejo de **409**.
7. **Nueva reserva**: validaciones extra (fecha no pasada, cantidad 1 para `TIME_EXCLUSIVE`), mensajes separados para **disponibilidad** vs **conflictos HTTP 409** al crear.
8. **UI homogénea**: `PageHeader`, `ConfirmDialog`, `Spinner`, `EmptyState`, `getApiErrorMessage` (incl. **409**).
9. **Nueva ruta** `/provider/reservations` para listado de reservas del prestador (backend existente).

---

## Archivos creados

| Archivo | Rol |
|---------|-----|
| `frontend/src/features/auth/RoleRoute.tsx` | Exige sesión + al menos un rol de la lista (OR). |
| `frontend/src/features/auth/ProviderRequestGate.tsx` | Exige USER; redirige PROVIDER al panel; ADMIN a acceso denegado con mensaje. |
| `frontend/src/features/auth/AccessDeniedPage.tsx` | Pantalla `/access-denied`. |
| `frontend/src/components/ui/PageHeader.tsx` | Título + highlight + subtítulo. |
| `frontend/src/components/ui/ConfirmDialog.tsx` | Diálogo modal reutilizable. |
| `frontend/src/features/provider/pages/ProviderDashboardPage.tsx` | Panel prestador. |
| `frontend/src/features/provider/pages/ProviderReservationsPage.tsx` | Reservas en sucursales del provider. |
| `frontend/src/features/provider/pages/ProviderRequestPage.tsx` | Solicitud ser prestador. |
| `frontend/src/features/admin/pages/AdminProviderRequestsPage.tsx` | Admin solicitudes. |
| `docs/sprint-6-frontend-estado.md` | Este documento. |

## Archivos modificados (principales)

- `frontend/src/app/router.tsx` — rutas con `RoleRoute`, `ProviderRequestGate`, `/access-denied`.
- `frontend/src/components/layout/AppLayout.tsx` — enlaces por rol (incl. «Reservas sucursales»).
- `frontend/src/features/booking/pages/CreateReservationPage.tsx` — validaciones y mensajes de error.
- `frontend/src/features/booking/pages/MyReservationsPage.tsx` — `PageHeader` + `ConfirmDialog`.
- `frontend/src/api/booking.api.ts` — `getProviderReservations`.
- `frontend/src/api/provider.api.ts` — `setBranchActive`, tipo `AdminProviderRequestStatus`, `getAdminProviderRequests` sin query vacío.
- `frontend/src/utils/apiError.ts` — tratamiento explícito de **409**.

## Archivos eliminados

- `frontend/src/features/auth/ProtectedRoute.tsx` (sustituido por `RoleRoute` + gates específicos).
- `frontend/src/pages/ProviderDashboard.tsx`
- `frontend/src/pages/ProviderRequestPage.tsx`
- `frontend/src/pages/AdminProviderRequestsPage.tsx`

*(La carpeta `src/pages` puede quedar vacía.)*

---

## Rutas / páginas

| Ruta | Guard | Contenido |
|------|--------|-----------|
| `/` | — | Home catálogo |
| `/login`, `/register` | — | Auth |
| `/access-denied` | — | Mensaje de acceso no permitido |
| `/reservations` | `RoleRoute` **USER** | Mis reservas |
| `/reservations/new` | `RoleRoute` **USER** | Nueva reserva |
| `/provider` | `RoleRoute` **PROVIDER** | Panel prestador |
| `/provider/reservations` | `RoleRoute` **PROVIDER** | Reservas de sus sucursales |
| `/provider-request` | `ProviderRequestGate` | Solicitud prestador |
| `/admin/provider-requests` | `RoleRoute` **ADMIN** | Gestión solicitudes |

---

## Endpoints usados (vía gateway)

**Catálogo (provider)** — `ProviderController` (`@PreAuthorize` incluye `PROVIDER` y `ADMIN` en backend; el front restringe el panel a **PROVIDER**):

- `GET /api/catalog/provider/branches`
- `POST /api/catalog/provider/branches`
- `PUT /api/catalog/provider/branches/{id}`
- `DELETE /api/catalog/provider/branches/{id}`
- `PATCH /api/catalog/provider/branches/{id}/active` — cuerpo `{ "active": boolean }`
- `POST /api/catalog/provider/branches/{branchId}/rooms` — cuerpo `RoomCreateRequest`: `name`, `hourlyPrice` (positivo)

**Catálogo (público / filtro salas):**

- `GET /api/catalog/items?branchId=&type=ROOM`

**Booking (provider):**

- `GET /api/booking/provider/reservations`

**Auth — solicitud prestador:**

- `GET /auth/provider-requests/me`
- `POST /auth/provider-requests` — **409** con `{ message }` en conflictos de negocio

**Admin:**

- `GET /admin/provider-requests` — opcional `?status=PENDING|APPROVED|REJECTED` (sin parámetro = todas)
- `POST /admin/provider-requests/{id}/approve` — **409** posible
- `POST /admin/provider-requests/{id}/reject` — **409** posible

**Booking (usuario)** — sin cambios de contrato respecto al Sprint 5:

- `POST /api/booking/availability`, `POST /api/booking/reservations`, etc.

---

## Cómo probar

1. Levantar stack con **gateway** en `8080` y servicios auth, catalog, booking.
2. `frontend`: `npm run dev` (CORS ya definido en gateway para `5173`).
3. **Usuario USER**: home, mis reservas, nueva reserva, «Ser prestador» (si no es ADMIN/PROVIDER).
4. **Tras aprobar solicitud**: cerrar sesión y volver a entrar — el JWT debe incluir **ROLE_PROVIDER** para ver panel y reservas de sucursales.
5. **PROVIDER**: panel (CRUD sucursal, toggle activo, crear sala), `/provider/reservations`.
6. **ADMIN**: `/admin/provider-requests`, filtros, aprobar/rechazar; verificar mensaje en **409** si la solicitud ya fue procesada.
7. **Acceso denegado**: usuario sin **USER** intentando `/provider-request`; **ADMIN** en `/provider-request`; usuario sin **USER** en `/reservations` → `/access-denied` o redirección según guard.
8. **Nueva reserva**: ítem `TIME_EXCLUSIVE` con cantidad ≠ 1 debe marcar error de campo; fecha de inicio en el pasado rechazada; simular carrera tras availability OK para ver mensaje de **409** al crear (si el backend lo devuelve).

---

## Supuestos y limitaciones

- **JWT y roles**: Tras aprobar prestador, el usuario debe **volver a iniciar sesión** para obtener un token con `ROLE_PROVIDER` (comportamiento ya documentado en UI; no hay refresh token en este front).
- **Panel `/provider`**: Reservado a **PROVIDER**. Un **ADMIN** sin rol PROVIDER no ve el enlace; si accediera por URL, el guard lo envía a `/access-denied`. (El backend permite `ADMIN` en algunos endpoints de `ProviderController`; el producto prioriza separación UX.)
- **Nombre de sucursal en reservas**: Las APIs de booking devuelven `branchId` numérico, no el nombre de la sucursal; el listado muestra ID (dato real del backend).
- **Cancelación desde provider**: No existe endpoint en el booking-service revisado para que el prestador cancele reservas del cliente; la pantalla de provider es **solo lectura** para ese listado.
- **Carpeta `src/pages`**: Quedó sin archivos tras el traslado; se puede borrar en el IDE si molesta.

---

*Sprint 6 cerrado en línea con el código y `npm run build` OK.*
