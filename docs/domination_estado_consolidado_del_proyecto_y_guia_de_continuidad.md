# DOMINation — Estado consolidado del proyecto y guía de continuidad

## 1. Propósito de este documento

Este documento consolida lo recopilado en los chats del proyecto y en la documentación técnica disponible, con el objetivo de:

* dejar una fuente de verdad operativa del estado actual del sistema,
* facilitar continuidad entre chats,
* alinear el trabajo entre ChatGPT, Codex, Cursor y el desarrollo manual,
* evitar volver a discutir decisiones ya tomadas.

Este texto debe tratarse como referencia de continuidad del proyecto full-stack.

---

## 2. Identidad del proyecto

**Nombre:** DOMINation V2
**Tipo de sistema:** plataforma de gestión de reservas de salas de ensayo, instrumentos y sucursales.
**Arquitectura:** microservicios con API Gateway + frontend React + PostgreSQL por servicio.
**Enfoque actual:** construir una base sólida, usable y observable, priorizando funcionalidad real antes que complejidad prematura.

---

## 3. Visión funcional del producto

DOMINation está orientado a una aplicación de reservas para un público abierto, donde:

* los **clientes** pueden consultar sucursales e ítems, autenticarse y reservar,
* los **providers** gestionan sus sucursales y reciben/administran reservas de su negocio,
* los **administradores** tienen visión y control global.

### Decisión importante ya tomada

Se discutió el uso de **BPM** en el proyecto. La conclusión fue que **no es una prioridad actual** ni parece prudente incorporarlo en esta etapa.

### Motivo

El sistema no está orientado a procesos internos empresariales complejos tipo ERP, sino a una operación de negocio relativamente directa:

* providers gestionan sucursales,
* clientes reservan salas o instrumentos,
* administradores supervisan,
* pagos y reservas siguen flujos bastante lineales.

### Criterio adoptado

* **No incorporar BPM por ahora.**
* Priorizar primero:

  * experiencia de usuario,
  * consistencia del dominio,
  * seguridad,
  * flujo de reservas,
  * observabilidad,
  * escalabilidad razonable.

Si más adelante aparecen procesos largos, aprobaciones complejas, automatizaciones multi-etapa o integraciones pesadas, se reevalúa.

---

## 4. Stack tecnológico acordado

### Backend

* **Java 21**
* **Spring Boot**
* **Spring Security**
* **Spring Cloud Gateway**
* **OAuth2 / JWT**
* **PostgreSQL**
* **Maven**
* **Docker Compose**

### Frontend

* **React 18**
* **TypeScript**
* **Vite**

### Observabilidad

* **Prometheus**
* **Grafana**
* Actuator / métricas / health checks / request-id tracing

### Estilo arquitectónico

* microservicios
* separación por responsabilidades
* DB por servicio
* consumo desde frontend exclusivamente vía **gateway**

---

## 5. Estado actual del sistema

Según la documentación consolidada y los chats del proyecto, el sistema ya cuenta con una base funcional real.

### Servicios activos del backend

* **auth-service** — puerto `9000`
* **catalog-service** — puerto `8081`
* **booking-service** — puerto `8082`
* **gateway** — puerto `8080`

### Frontend

* **React + TypeScript + Vite** — puerto `5173`

### Infraestructura local

* PostgreSQL catalog — `5432`
* PostgreSQL booking — `5433`
* PostgreSQL auth — `5434`
* Prometheus — `9090`
* Grafana — `3000`

---

## 6. Arquitectura funcional actual

### 6.1 Frontend

Responsabilidades actuales/esperadas:

* interfaz de usuario,
* autenticación y manejo de sesión,
* consumo de APIs del backend únicamente a través del gateway,
* visualización de catálogo, reservas y funcionalidades protegidas.

### 6.2 API Gateway

Responsabilidades:

* punto de entrada único,
* enrutamiento de requests,
* CORS para el frontend,
* propagación y/o generación de `X-Request-Id`,
* exposición de métricas.

### 6.3 Auth Service

Responsabilidades:

* registro de usuarios,
* login,
* emisión de tokens JWT,
* manejo de roles,
* publicación de JWKS.

### 6.4 Catalog Service

Responsabilidades:

* sucursales,
* ítems,
* inventario,
* disponibilidad,
* hold y release de inventario para reservas,
* soporte a stock real en integración con booking-service.

### 6.5 Booking Service

Responsabilidades:

* creación de reservas,
* listado de reservas del usuario,
* listado de reservas del provider,
* cancelación,
* validación de conflictos.

---

## 7. Endpoints funcionales ya existentes

### Públicos

* `GET /api/catalog/branches`
* `GET /api/catalog/items`

### Protegidos

* `POST /api/booking/availability` *(requiere JWT / ROLE_USER según controller actual)*
* `POST /api/booking/reservations`
* `GET /api/booking/my/reservations`
* `GET /api/booking/provider/reservations`
* `POST /api/booking/reservations/{id}/cancel`
* `POST /api/catalog/inventory/hold`
* `POST /api/catalog/inventory/release`

### Auth

* `POST /auth/register`
* `POST /auth/login`
* `GET /oauth2/jwks`

### Actuator

* `GET /actuator/health`
* `GET /actuator/prometheus`

---

## 8. Reglas y comportamiento de negocio ya definidos

### 8.1 Autenticación

El sistema ya cuenta con autenticación completa integrada con `auth-service`.

Existe un usuario administrador sembrado automáticamente:

* **usuario:** `adminSeba`
* **contraseña:** `123456admin`
* **roles:** `ROLE_ADMIN`, `ROLE_USER`

> En producción esta contraseña debe cambiarse obligatoriamente.

### 8.2 Registro

El flujo contemplado permite:

* registrar usuario,
* iniciar sesión luego del registro,
* usar JWT para operaciones protegidas.

### 8.3 Reservas

El sistema permite:

* crear reserva,
* listar reservas propias,
* listar reservas de provider,
* cancelar reserva.

### 8.4 Validación de conflictos y disponibilidad

El consolidado originalmente reflejaba una decisión de etapa temprana:

* **Modo A: validación lógica**,
* control de solapamiento de horarios,
* control de cantidad reservada vs disponibilidad,
* sin manejo completo de stock físico real.

Ese criterio fue válido como base inicial, pero el estado más actualizado del proyecto ya avanzó más.

### Estado actualizado

A partir de los avances de **Sprint 3** y **Sprint 4**, el sistema ya incorpora:

* **pre-check de disponibilidad** mediante `POST /api/booking/availability`,
* DTOs de disponibilidad específicos,
* integración de **hold/release** de inventario entre booking-service y catalog-service,
* persistencia de `holdId` en las líneas de reserva,
* release automático al cancelar reservas.

### Criterio vigente de continuidad

* La validación lógica sigue existiendo como parte del control de negocio.
* Pero el proyecto ya no debe documentarse solamente como “stock lógico para una fase posterior”.
* El estado actual debe asumirse como una transición hacia **stock real controlado mediante holds temporales**, al menos para los flujos ya integrados.

### 8.5 Cancelación

* el endpoint de cancelación es **idempotente**,
* retorna `200`,
* al cancelar una reserva debe contemplarse además la liberación del inventario retenido cuando exista `holdId` asociado.

### 8.6 Manejo de conflictos

* ante conflictos de reserva, el sistema retorna `409 Conflict` con body estructurado.

---

## 9. Estado del backend según avance en chats

### Ya validado

* swagger funcionando correctamente,
* endpoints principales respondiendo bien,
* integración de reservas operativa,
* documentación actualizada tras cambios,
* sistema corriendo con Docker,
* frontend y backend ya convivieron en una base funcional,
* Sprint 3 avanzó con pre-check de disponibilidad,
* Sprint 4 avanzó con hold/release de inventario e integración booking-catalog.

### Conversación relevante sobre tablas/bases

Se conversó sobre cómo inspeccionar las tablas de las bases que corren en Docker y sobre la posibilidad de usar **pgAdmin** para conectarse.
También apareció una duda puntual sobre no ver la tabla `inventory_hold` en la base de catalog.

### Interpretación útil para continuidad

* el proyecto ya está en una etapa donde conviene validar estructura de datos visualmente,
* es razonable seguir usando herramientas como pgAdmin o clientes PostgreSQL para inspección,
* no hay que asumir que todas las tablas “pensadas” ya están materializadas; hay que contrastar con migraciones/entidades/ddl.

---

## 10. Problemas técnicos ya detectados y cómo quedaron resueltos

### 10.1 CORS

Problema detectado:

* el frontend sufría `Failed to fetch` por configuración CORS.

Solución adoptada:

* configurar CORS global en el **gateway** vía `application.properties`,
* permitir `http://localhost:5173` y `http://127.0.0.1:5173`,
* exponer headers necesarios,
* deduplicar headers CORS.

### Regla operativa importante

El frontend debe consumir:

* `VITE_API_BASE_URL=http://localhost:8080`

Y **no** llamar directamente a los microservicios internos.

### 10.2 JWT / autenticación

Problemas contemplados:

* token expirado,
* JWT inválido,
* auth-service no accesible,
* JWKS no disponible.

### Regla operativa

El frontend debe contemplar:

* manejo de `401`,
* expiración de sesión,
* re-login,
* no dejar vistas rotas cuando el token vence.

### 10.3 Request-Id tracing

Ya existe una estrategia de trazabilidad por request:

* gateway genera o propaga `X-Request-Id`,
* servicios lo incorporan al MDC,
* logs muestran requestId,
* la respuesta vuelve con el mismo identificador.

Esto es importante para debugging, observabilidad y soporte.

---

## 11. Observabilidad ya disponible

La solución ya contempla una base seria de observabilidad.

### Componentes

* Prometheus
* Grafana
* Spring Actuator
* métricas Prometheus
* health checks
* tracing por request-id

### Objetivo práctico

No solo correr servicios, sino poder:

* verificar si están arriba,
* medir tasa de requests,
* detectar errores 5xx,
* revisar uso de memoria y CPU,
* construir dashboards.

### Decisión de madurez

La observabilidad no es un “nice to have”; forma parte de la base del proyecto.

---

## 12. Sprints y secuencia de avance ya discutida

En conversaciones previas se avanzó por sprints, incluyendo:

* testing de booking-service,
* actualización de documentación,
* revisión de tablas/DB,
* intención de continuar con Sprint 3, 4 y luego 5,
* uso de Cursor para acelerar partes del trabajo.

### Criterio que quedó implícito

* avanzar por entregables cortos,
* validar con Swagger y ejecución real,
* documentar cada paso,
* no saltar a complejidad innecesaria.

---

## 13. Dirección acordada para el frontend

Se decidió avanzar con el frontend **ya**, aunque el backend siga evolucionando en otros chats.

### Criterio central

El frontend no debe esperar a que el backend esté “terminado”.
Debe empezar a **visualizar lo que el backend ya expone**.

Esto permite:

* validar integración real,
* detectar huecos de API,
* probar flujos de usuario de punta a punta,
* convertir backend funcional en producto visible.

### Enfoque senior UX/UI adoptado

No construir un front “de maqueta”.
Construir una interfaz que refleje el estado real del sistema actual.

---

## 14. Qué debe mostrar ya el frontend

### Mínimo producto visual funcional

1. **Home pública**

   * presentación de la app,
   * listado de sucursales,
   * listado de ítems,
   * CTA a login/registro.

2. **Login**

   * username/password,
   * feedback claro,
   * guardado de token,
   * redirección posterior.

3. **Registro**

   * username,
   * email,
   * password,
   * auto-login si el flujo lo permite.

4. **Dashboard de usuario**

   * acceso a reservas,
   * acceso a crear reserva,
   * visibilidad de estado de sesión.

5. **Mis reservas**

   * listado,
   * estado,
   * cancelación.

6. **Vista provider**

   * reservas de la sucursal/provider,
   * filtros básicos por estado/fecha.

### Valor de esta decisión

Con estas pantallas ya se puede visualizar gran parte del backend real sin inventar features que todavía no existen.

---

## 15. Rutas de frontend recomendadas

Las rutas sugeridas para empezar son:

* `/`
* `/login`
* `/register`
* `/branches`
* `/items`
* `/dashboard`
* `/my-reservations`
* `/provider/reservations`

---

## 16. Arquitectura recomendada del frontend

Se propuso una arquitectura ordenada por features:

```text
frontend/src
  /app
    router.tsx
    providers.tsx
  /api
    axios.ts
    auth.api.ts
    catalog.api.ts
    booking.api.ts
  /features
    /auth
    /catalog
    /booking
  /components
    /layout
    /ui
  /pages
  /hooks
  /types
  /utils
```

### Stack recomendado para front

* React Router
* Axios
* TanStack Query
* Zustand o Context para auth
* React Hook Form + Zod
* Tailwind o CSS Modules

### Regla importante

No dejar el frontend centralizado en un `App.tsx` gigante y desordenado.
La estructura debe quedar preparada para crecimiento.

---

## 17. Backlog frontend recomendado

### Sprint UI-1

* layout base,
* navbar,
* login,
* register,
* guards de ruta,
* manejo global del token.

### Sprint UI-2

* listado de sucursales,
* listado de ítems,
* estados loading/error/empty,
* detalle visual básico.

### Sprint UI-3

* crear reserva,
* listar mis reservas,
* cancelar reserva,
* toasts de éxito/error.

### Sprint UI-4

* vista provider,
* filtros,
* badges de estado,
* responsive y refinamientos.

---

## 18. Criterios UX/UI ya establecidos

### Principios visuales

* producto limpio, no interfaz improvisada,
* jerarquía tipográfica clara,
* spacing generoso,
* cards limpias,
* pocos colores pero bien usados,
* badges de estado,
* feedback inmediato,
* estados vacíos y de error cuidados.

### Componentes recomendados

* `AppShell`
* `PageHeader`
* `StatCard`
* `DataTable`
* `EmptyState`
* `ErrorState`
* `LoadingSkeleton`
* `ProtectedRoute`
* `RoleGate`

### Buenas prácticas UX

* siempre mostrar loading cuando corresponda,
* no dejar al usuario sin feedback,
* errores humanos y claros,
* confirmar cancelaciones,
* no exponer IDs internos sin necesidad,
* usar fechas legibles.

---

## 19. Decisiones de producto/arquitectura que deben respetarse

1. **Frontend consume solo por gateway.**
2. **No meter BPM en esta etapa.**
3. **No introducir complejidad de ERP que el dominio todavía no necesita.**
4. **Priorizar experiencia real sobre features teóricas.**
5. **Mantener arquitectura modular en backend y frontend.**
6. **Documentar cambios importantes a medida que se consolidan.**
7. **Validar con ejecución real (Swagger / UI / DB / logs), no solo por teoría.**
8. **Mantener observabilidad y trazabilidad como parte del estándar del proyecto.**

---

## 20. Qué debería hacer Codex/Cursor al continuar

Cuando Codex/Cursor retome el proyecto, debe asumir como contexto base lo siguiente:

### Contexto técnico

* stack: Java 21 / Spring / PostgreSQL / React TS / microservicios,
* arquitectura por capas + servicios desacoplados,
* gateway como puerta única,
* auth con JWT,
* frontend en Vite,
* observabilidad ya presente.

### Contexto funcional

* sistema de reservas de salas/ítems por sucursal,
* roles al menos: admin, user, provider,
* catálogo público,
* reservas protegidas por autenticación,
* provider con vista específica,
* disponibilidad consultable antes de reservar,
* integración de hold/release para stock real en los flujos ya evolucionados.

### Contexto de producto

* UX/UI debe acompañar el backend real,
* nada de overengineering prematuro,
* BPM descartado por ahora,
* priorizar continuidad incremental.

### Comportamiento esperado de la asistencia automática

* no reinventar arquitectura ya acordada,
* no romper el consumo vía gateway,
* no duplicar lógica ya existente,
* proponer cambios en sprints pequeños,
* justificar cada cambio importante,
* dejar todo alineado con documentación.

---

## 21. Próximos pasos recomendados

### Línea A — Frontend

1. inspeccionar estructura actual del frontend,
2. ordenar carpetas por features,
3. implementar router/base layout,
4. resolver auth client-side,
5. conectar catálogo público,
6. conectar reservas del usuario,
7. conectar vista provider,
8. refinar experiencia visual.

### Línea B — Backend

1. seguir endureciendo validaciones,
2. revisar consistencia de tablas/entidades,
3. validar casos borde de reservas,
4. reforzar roles/permisos,
5. endurecer la gestión de inventario real: expiración de holds, edge cases de cancelación/liberación y alineación de reglas temporales si aplica.

### Línea C — Integración

1. probar flujos end-to-end,
2. validar errores reales desde frontend,
3. medir comportamiento con métricas/logs,
4. documentar cada sprint importante.

---

## 22. Riesgos a evitar

* acoplar frontend directo a microservicios,
* meter features no respaldadas por backend,
* introducir BPM sin necesidad real,
* crecer sin una estructura de frontend mantenible,
* confiar en supuestos sin validar con Swagger/DB/logs,
* dejar el manejo de auth/JWT improvisado,
* no contemplar estados de error y carga en UI.

---

## 23. Estado de madurez actual del proyecto

El proyecto ya dejó de ser una idea abstracta.
Tiene:

* base técnica definida,
* arquitectura consistente,
* autenticación funcional,
* reservas funcionales,
* catálogo funcional,
* observabilidad incorporada,
* documentación técnica inicial,
* dirección clara para la siguiente etapa del frontend.

La siguiente evolución lógica no es agregar exotismo técnico, sino:

* consolidar la experiencia de usuario,
* visualizar mejor el backend,
* estabilizar flujos,
* iterar con criterio.

---

## 24. Resumen ejecutivo para retomar rápido

Si cualquier herramienta o desarrollador retoma DOMINation, debe partir de esta base:

* Es una plataforma de reservas de salas/instrumentos por sucursal.
* Usa microservicios con gateway, auth-service, catalog-service y booking-service.
* El frontend está en React + TypeScript + Vite.
* El backend ya expone catálogo público, pre-check de disponibilidad y endpoints protegidos de reservas.
* Hay JWT, roles y usuario admin sembrado.
* Hay Prometheus, Grafana, actuator y request-id tracing.
* Ya hubo problemas de CORS y quedaron resueltos en gateway.
* El frontend debe avanzar ahora para mostrar lo que backend ya ofrece, incluyendo disponibilidad previa a la reserva y el comportamiento ligado a stock real/holds cuando corresponda.
* BPM no se incorpora en esta etapa.
* El trabajo debe continuar de forma incremental, documentada y sin sobreingeniería.

---

## 25. Instrucción de continuidad recomendada para asistentes de código

**Trabajá sobre el estado real del proyecto, no sobre supuestos.**
**No rompas la arquitectura ya definida.**
**Consumí todo desde el gateway.**
**Priorizá UX útil, integración real y cambios incrementales.**
**Toda propuesta debe ser coherente con la documentación y con los endpoints ya disponibles.**
