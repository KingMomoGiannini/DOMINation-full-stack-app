# AGENTS.md

## Propósito

Este archivo define las reglas de trabajo para agentes de IA que colaboren sobre este repositorio.  
El objetivo es que cualquier agente produzca cambios coherentes con la arquitectura, el stack, las convenciones y el roadmap del proyecto.

Este proyecto es **DOMINation**, una aplicación de gestión de reservas de salas de ensayo con arquitectura de microservicios.

---

## Objetivo del sistema

La aplicación permite:

- gestionar sucursales
- gestionar inventario de salas e instrumentos
- consultar disponibilidad
- registrar usuarios
- autenticarse con JWT
- crear, listar y cancelar reservas
- diferenciar permisos por rol
- operar a través de un API Gateway

---

## Stack oficial

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Cloud Gateway
- OAuth2 / JWT
- Maven
- PostgreSQL

### Frontend
- React 18
- TypeScript
- Vite

### Infraestructura y observabilidad
- Docker Compose
- Prometheus
- Grafana

---

## Estructura general del monorepo

- `frontend/` → aplicación React + TypeScript + Vite
- `gateway/` → API Gateway con Spring Cloud Gateway
- `services/auth-service/` → autenticación, usuarios, JWT
- `services/catalog-service/` → sucursales, items, disponibilidad, inventario
- `services/booking-service/` → reservas, conflictos, cancelaciones
- `infra/` → docker-compose, Prometheus y configuración de entorno
- `docs/` → documentación técnica del proyecto

---

## Arquitectura obligatoria

### Principios
- Respetar arquitectura en capas.
- No mezclar responsabilidades entre controller, service, repository y configuración.
- No mover lógica de negocio al controller.
- No acceder directamente a la base desde controllers.
- Mantener separación clara entre microservicios.
- Cada microservicio es dueño de su propia base de datos.

### Capas esperadas en backend
Usar, cuando aplique:
- controller
- service
- repository
- entity
- dto
- mapper
- config
- exception

### Reglas
- Los controllers reciben requests y delegan.
- Los services implementan lógica de negocio.
- Los repositories resuelven persistencia.
- Los DTO se usan para contratos de entrada/salida cuando corresponda.
- Las excepciones deben mapearse de forma consistente.

---

## Microservicios y responsabilidades

### auth-service
Responsable de:
- registro de usuarios
- login
- emisión de JWT access + refresh
- gestión de roles
- endpoint JWKS para validación de tokens

No debe contener lógica de reservas ni catálogo.

### catalog-service
Responsable de:
- sucursales
- items
- inventario
- disponibilidad
- mecanismos relacionados con stock/hold si el sprint actual lo requiere

No debe contener autenticación propia ni lógica principal de reservas.

### booking-service
Responsable de:
- creación de reservas
- consulta de reservas del usuario
- consulta de reservas del provider
- cancelación idempotente
- validación de conflictos
- integración con catalog-service para disponibilidad y/o hold/release según sprint

No debe duplicar la lógica fuente del catálogo más allá de lo necesario para integrarse.

### gateway
Responsable de:
- punto de entrada único
- routing
- CORS
- propagación de headers
- Request-Id

No debe contener lógica de negocio de dominio.

### frontend
Responsable de:
- UI
- gestión de sesión del usuario
- consumo de APIs vía gateway
- flujos de login, registro, catálogo y reservas

---

## Seguridad

### Reglas obligatorias
- La autenticación se resuelve con JWT emitido por `auth-service`.
- `catalog-service` y `booking-service` son resource servers.
- No eliminar validaciones JWT existentes.
- No exponer endpoints protegidos sin justificación explícita.
- Si se modifica seguridad, revisar compatibilidad con Swagger y con el flujo actual del frontend.

### Roles
Respetar roles existentes y su intención:
- `ROLE_USER`
- `ROLE_PROVIDER`
- `ROLE_ADMIN`

No inventar roles nuevos salvo pedido explícito.

### Endpoints públicos y protegidos
Antes de cambiar seguridad:
- verificar si el endpoint debe ser público o protegido
- respetar contratos ya documentados
- evitar contradicciones entre código y documentación

---

## Contratos API y HTTP

### Reglas
- Mantener consistencia en rutas y códigos HTTP.
- `POST` de creación debe devolver `201` cuando corresponda.
- Cancelaciones idempotentes deben mantenerse como idempotentes.
- En conflictos de negocio devolver `409 Conflict` cuando aplique.
- No romper contratos existentes sin documentarlo.

### Errores
Usar respuestas de error claras, consistentes y estructuradas.  
Si ya existe un formato de error, reutilizarlo.

---

## Base de datos

### Reglas generales
- Una base por microservicio.
- No compartir tablas entre servicios.
- No acoplar entidades entre microservicios.
- No asumir que un servicio puede consultar directamente la DB de otro.

### Cambios de esquema
- Todo cambio de modelo debe ser coherente con el servicio dueño.
- Si se agrega una entidad, revisar relaciones, constraints e impacto en tests.
- No eliminar tablas/campos sin evaluar compatibilidad con datos y código actual.
- Si hay un mecanismo ya existente de `ddl-auto`, no asumir migraciones automáticas fuera de lo ya configurado.

### PostgreSQL
Respetar configuración actual de puertos, usuarios y bases del proyecto.

---

## Integración entre servicios

### Reglas
- La comunicación entre servicios debe ser explícita y coherente con la arquitectura actual.
- `booking-service` puede consultar `catalog-service` para disponibilidad o inventario según el flujo implementado.
- No crear dependencias cruzadas innecesarias.
- No duplicar lógica de negocio distribuida si ya existe una fuente clara de verdad.

### Holds / release / disponibilidad
Si el sprint requiere stock real, hold/release o availability:
- mantener coherencia con el estado ya implementado
- no mezclar “modo lógico” con “stock real” sin aclararlo
- si se cambia el flujo, actualizar documentación y contratos

---

## Observabilidad y trazabilidad

### Reglas obligatorias
- No romper Actuator.
- No romper `/actuator/health`.
- No romper `/actuator/prometheus`.
- Mantener integración con Prometheus y Grafana.
- Preservar propagación de `X-Request-Id`.

### Logging
- Mantener logs útiles para debugging.
- No eliminar correlación por requestId.
- Evitar logs excesivamente verbosos en producción salvo necesidad real.

---

## Swagger / OpenAPI

### Reglas
- Si un endpoint protegido debe probarse en Swagger, mantener esquema Bearer JWT.
- No romper `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs` o equivalentes ya habilitados.
- Si se cambia seguridad, verificar que Swagger siga funcionando.
- Si se modifica el scheme de seguridad, alinear `SecurityRequirement` y `SecurityScheme`.

---

## Frontend

### Reglas
- Usar TypeScript correctamente.
- No introducir `any` innecesario.
- Mantener consistencia con la estructura actual del frontend.
- Consumir APIs a través del gateway, no directamente a microservicios salvo instrucción explícita.
- Mantener la experiencia visual consistente con el diseño existente.

### Estado y sesión
- Respetar el flujo actual de login y almacenamiento de token.
- No romper navegación autenticada.
- Si se cambia un contrato backend, reflejarlo en frontend.

---

## Docker y entorno local

### Regla principal
Para desarrollo cotidiano, asumir que el proyecto se levanta con Docker Compose desde `infra/`.

### Comportamiento esperado de un agente
Cuando proponga comandos, priorizar:
```bash
cd infra
docker-compose up -d