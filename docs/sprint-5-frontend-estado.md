# Sprint 5 — Estado: frontend alineado al backend (DOMINation V2)

Documento de cierre del **Sprint 5**: base funcional del frontend consumiendo el backend real **exclusivamente vía API Gateway** (`http://localhost:8080` por defecto).

---

## Objetivo cumplido

- Estructura mantenible por **features** y capas (`app`, `api`, `features/*`, `components`, `types`, `utils`).
- **React Router** con layout común y rutas públicas / protegidas.
- **Auth JWT**: login, registro, persistencia en `localStorage`, manejo de **401 / sesión expirada**.
- **Home pública**: sucursales e ítems desde catálogo real.
- **Mis reservas** (usuario autenticado con `ROLE_USER`).
- **Nueva reserva**: pre-check `POST /api/booking/availability` y luego creación de reserva.
- **Cancelación** de reserva con confirmación en UI.
- Estados **loading**, **empty**, **error** y mensajes orientados al usuario.

---

## Gateway (cambio imprescindible)

Para que login, registro, solicitudes de prestador y admin no llamen directo al puerto 9000, el gateway expone:

| Ruta            | Destino                         |
|-----------------|----------------------------------|
| `/auth/**`      | `http://auth-service:9000`       |
| `/admin/**`     | `http://auth-service:9000`       |

Definición en `gateway/src/main/resources/application.properties` (rutas índices 2 y 3).

**Nota:** con Docker Compose el hostname `auth-service` es correcto. Si corrés el gateway en el host y auth en local, ajustá la URI a `http://localhost:9000` (u host equivalente).

---

## Dependencias frontend añadidas

- `axios`
- `@tanstack/react-query`
- `react-hook-form`, `@hookform/resolvers`, `zod`

Stack existente: React 18, TypeScript, Vite, `react-router-dom`.

---

## Estructura relevante (`frontend/src`)

```
app/
  router.tsx          — definición de rutas
api/
  http.ts             — instancia Axios + interceptores (Bearer, 401)
  authStorage.ts      — token, usuario, roles, userId en localStorage
  auth.api.ts         — login / register vía gateway
  catalog.api.ts      — branches / items
  booking.api.ts      — availability, CRUD reservas usuario, cancel
  provider.api.ts     — panel prestador + provider-requests + admin (gateway)
  index.ts            — barrel
  apiClient.ts        — re-export hacia compatibilidad con imports antiguos
features/
  auth/               — AuthContext, ProtectedRoute, LoginPage, RegisterPage
  catalog/pages/      — HomePage
  booking/pages/      — MyReservationsPage, CreateReservationPage
components/
  layout/AppLayout.tsx
  ui/Spinner.tsx, EmptyState.tsx
types/
  auth.ts, catalog.ts, booking.ts
utils/
  apiError.ts         — mensajes legibles desde errores Axios/API
```

Páginas legacy no movidas en profundidad: `pages/ProviderDashboard.tsx`, `ProviderRequestPage.tsx`, `AdminProviderRequestsPage.tsx` (usan `features/auth/AuthContext` y APIs del barrel).

---

## Rutas SPA

| Ruta | Descripción |
|------|-------------|
| `/` | Home pública (catálogo) |
| `/login` | Login |
| `/register` | Registro (+ login automático) |
| `/reservations` | Mis reservas (protegida) |
| `/reservations/new` | Nueva reserva (protegida) |
| `/provider` | Panel prestador |
| `/provider-request` | Solicitud ser prestador |
| `/admin/provider-requests` | Admin solicitudes |

En la barra superior, **Mis reservas** y **Nueva reserva** se muestran si el usuario tiene rol **USER** (`hasRole('USER')`), alineado con los endpoints de booking.

---

## Endpoints consumidos (vía gateway)

- `POST /auth/register`
- `POST /auth/login`
- `GET /api/catalog/branches`
- `GET /api/catalog/items`
- `POST /api/booking/availability` (JWT, `ROLE_USER`)
- `POST /api/booking/reservations`
- `GET /api/booking/my/reservations`
- `POST /api/booking/reservations/{id}/cancel`

Además, para flujos ya existentes de prestador/admin: rutas bajo `/api/catalog/provider/...`, `/auth/provider-requests`, `/admin/provider-requests`, etc., siempre contra la misma base URL del gateway.

---

## Comportamiento destacado

### Sesión y 401

- El cliente Axios adjunta `Authorization: Bearer …` salvo en `POST /auth/login` y `POST /auth/register`.
- Ante **401** en el resto de llamadas: se limpia almacenamiento de sesión y se redirige a `/login?expired=1` (recarga completa para alinear estado React + `localStorage`).
- La pantalla de login muestra un aviso si llega `expired=1`.

### Nueva reserva

- Un solo envío del formulario: primero **availability**; si `available === true`, se llama a **create**; si no, mensaje claro y listado de **conflicts** cuando el backend los devuelve.

### Mis reservas

- Listado con TanStack Query; **cancelar** abre un diálogo de confirmación antes de `POST .../cancel`.

### Errores

- `utils/apiError.ts` prioriza `message`, `detail`, `title` del cuerpo y mensajes por código (401, 403, 404) para no mostrar solo texto crudo del servidor.

---

## Cómo ejecutar y probar

1. Levantar **gateway**, **auth-service**, **catalog-service** y **booking-service** (p. ej. `infra/docker-compose.yml`).
2. En `frontend`: `npm install` y `npm run dev` (puerto **5173**).
3. Opcional: variable `VITE_API_BASE_URL=http://localhost:8080` en `.env` o `.env.local`.

Verificación rápida: registro/login → home con datos reales → nueva reserva → listar y cancelar → forzar token inválido o esperar expiración y comprobar redirección a login.

---

## Supuestos y matices

- El **registro** en auth-service hoy crea usuario con **ROLE_USER**; el formulario permite elegir “Prestador” (`roleType: PROVIDER`) según contrato del DTO, pero el rol efectivo en BD lo define el backend (sin cambios de auth-service en este sprint).
- **Zustand** no se incorporó; la sesión UI sigue en **React Context** (`AuthProvider`).
- Build verificado: `npm run build` (tsc + vite) OK en el estado de este sprint.

---

## Referencia de archivos tocados (alto nivel)

- **Gateway:** `gateway/src/main/resources/application.properties`
- **Frontend:** `package.json`, `src/main.tsx`, `src/App.tsx`, `src/App.css`, `src/app/router.tsx`, módulos bajo `src/api/`, `src/types/`, `src/utils/apiError.ts`, `src/components/**`, `src/features/**`
- **Eliminados en favor de features:** `src/context/AuthContext.tsx`, páginas antiguas `Home`, `LoginPage`, `RegisterPage`, `CreateReservationPage` en `src/pages/`

---

*Última actualización: estado consolidado del Sprint 5 (frontend + rutas gateway para auth/admin).*
