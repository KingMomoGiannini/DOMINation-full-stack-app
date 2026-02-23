# Arquitectura del Sistema DOMINation

## Visión General

DOMINation es una aplicación de gestión de reservas de salas de ensayo construida con arquitectura de microservicios. El sistema está compuesto por:

- **Frontend**: Aplicación React con TypeScript
- **API Gateway**: Punto de entrada único para todas las peticiones
- **Microservicios**: Servicios especializados con bases de datos independientes
- **Infraestructura**: Docker Compose para orquestación local

## Componentes Principales

### 1. Frontend (React + TypeScript + Vite)

- **Puerto**: 5173
- **Tecnología**: React 18, TypeScript, Vite
- **Responsabilidades**:
  - Interfaz de usuario
  - Autenticación y gestión de sesión
  - Consumo de APIs vía Gateway

### 2. API Gateway (Spring Cloud Gateway)

- **Puerto**: 8080
- **Tecnología**: Spring Cloud Gateway (Reactive)
- **Responsabilidades**:
  - Enrutamiento de peticiones a microservicios
  - Configuración CORS para frontend
  - Generación y propagación de Request-Id
  - Exposición de métricas Prometheus

**Rutas configuradas**:
- `/api/catalog/**` → `catalog-service:8081`
- `/api/booking/**` → `booking-service:8082`

### 3. Auth Service (OAuth2 Authorization Server)

- **Puerto**: 9000
- **Tecnología**: Spring Boot, Spring Security OAuth2
- **Base de Datos**: PostgreSQL (puerto 5434)
- **Responsabilidades**:
  - Autenticación de usuarios
  - Emisión de tokens JWT (access + refresh)
  - Gestión de usuarios y roles
  - Endpoint JWKS para validación de tokens

**Endpoints principales**:
- `POST /auth/register` - Registro de usuarios
- `POST /auth/login` - Login y obtención de tokens
- `GET /oauth2/jwks` - Claves públicas para validación

### 4. Catalog Service

- **Puerto**: 8081
- **Tecnología**: Spring Boot, Spring Security (Resource Server)
- **Base de Datos**: PostgreSQL (puerto 5432)
- **Responsabilidades**:
  - Gestión de sucursales (branches)
  - Gestión de items (salas, instrumentos)
  - Gestión de inventario
  - Validación de disponibilidad

**Endpoints principales**:
- `GET /api/branches` - Listar sucursales (público)
- `GET /api/items` - Listar items (público)
- `GET /api/branches/{id}/availability` - Consultar disponibilidad

### 5. Booking Service

- **Puerto**: 8082
- **Tecnología**: Spring Boot, Spring Security (Resource Server)
- **Base de Datos**: PostgreSQL (puerto 5433)
- **Responsabilidades**:
  - Gestión de reservas (creación, listado, cancelación)
  - Validación de conflictos por solapamiento y cantidad reservada (Modo A: lógica)
  - Historial de reservas por usuario y por provider/sucursal

**Endpoints principales** (base path `/api/booking`):
- `POST /api/booking/reservations` - Crear reserva (requiere JWT) → 201
- `GET /api/booking/my/reservations` - Mis reservas (ROLE_USER)
- `GET /api/booking/provider/reservations` - Reservas del provider/sucursal (ROLE_PROVIDER)
- `POST /api/booking/reservations/{id}/cancel` - Cancelar reserva (idempotente) → 200

**Validación de conflictos**:
- En Modo A, la validación es **lógica**: solapamiento de horarios y cantidad reservada vs disponibilidad.
- El "stock real" (físico) se implementará en una fase posterior.
- En conflicto: retorna **409 Conflict** con body estructurado (`title`, `detail`, `status`, `instance`, `timestamp`).

**Swagger UI**: http://localhost:8082/swagger-ui.html — con Authorize (JWT Bearer) para probar endpoints protegidos.

## Flujo de Peticiones

### 1. Petición Pública (sin autenticación)

```
Frontend → Gateway:8080 → Catalog Service:8081 → PostgreSQL
```

Ejemplo: `GET /api/catalog/branches`

### 2. Petición Autenticada

```
Frontend → Gateway:8080 → Booking Service:8082 → Catalog Service:8081 → PostgreSQL
                ↓
         Auth Service:9000 (validación JWT)
```

Ejemplo: `POST /api/booking/reservations`

1. Frontend envía JWT en header `Authorization: Bearer <token>`
2. Gateway reenvía el token al servicio destino
3. El servicio valida el token contra Auth Service (JWKS)
4. Si es válido, procesa la petición

### 3. Flujo de Autenticación

```
Frontend → Auth Service:9000 → PostgreSQL (auth_db)
                ↓
         Retorna JWT (access + refresh)
                ↓
         Frontend guarda token en localStorage
```

## Seguridad

### OAuth2/JWT

- **Authorization Server**: `auth-service` (puerto 9000)
- **Resource Servers**: `catalog-service` y `booking-service`
- **Validación de tokens**: Vía JWKS endpoint (`/oauth2/jwks`)
- **Expiración**: Access token 1 hora, Refresh token 7 días

### CORS

Configurado en el Gateway para permitir:
- **Orígenes**: `http://localhost:5173`, `http://127.0.0.1:5173`
- **Métodos**: GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Headers**: Authorization, Content-Type, Accept, Origin, X-Requested-With
- **Credentials**: Habilitado

### Request-Id Tracing

- **Header**: `X-Request-Id`
- **Generación**: Gateway genera UUID si no existe
- **Propagación**: Se propaga a todos los servicios
- **Logs**: Se incluye en MDC para correlación

## Bases de Datos

Cada microservicio tiene su propia base de datos PostgreSQL:

| Servicio | Base de Datos | Puerto | Usuario | Contraseña |
|----------|---------------|--------|---------|------------|
| auth-service | `auth_db` | 5434 | postgres | postgres |
| catalog-service | `domination_catalog` | 5432 | domination | domination123 |
| booking-service | `domination_booking` | 5433 | domination | domination123 |

**Inicialización**: Hibernate con `spring.jpa.hibernate.ddl-auto=update`

## Observabilidad

### Prometheus

- **Puerto**: 9090
- **Scraping**: Cada 10 segundos
- **Targets**:
  - `gateway:8080/actuator/prometheus`
  - `catalog-service:8081/actuator/prometheus`
  - `booking-service:8082/actuator/prometheus`
  - `auth-service:9000/actuator/prometheus`

### Grafana

- **Puerto**: 3000
- **Credenciales**: admin/admin (cambiar en producción)
- **Datasource**: Prometheus (http://prometheus:9090)

### Métricas Expuestas

Todos los servicios exponen métricas estándar de Spring Boot Actuator:
- `http_server_requests_seconds` - Latencia de peticiones HTTP
- `jvm_memory_used_bytes` - Uso de memoria JVM
- `jvm_gc_pause_seconds` - Pausas de garbage collection
- `process_cpu_usage` - Uso de CPU

## Red

Todos los servicios se comunican a través de la red Docker `domination-network` (bridge).

**Nombres de host internos**:
- `gateway`
- `auth-service`
- `catalog-service`
- `booking-service`
- `postgres-auth`
- `postgres-catalog`
- `postgres-booking`
- `prometheus`
- `grafana`

## Dependencias entre Servicios

```
gateway
  ├── catalog-service (inicio)
  └── booking-service (inicio)

catalog-service
  └── postgres-catalog (healthy)

booking-service
  ├── postgres-booking (healthy)
  ├── catalog-service (inicio)
  └── auth-service (inicio)

auth-service
  └── postgres-auth (healthy)
```

## Configuración de Entornos

### Desarrollo (Docker Compose)

- Todos los servicios en contenedores
- Red interna Docker para comunicación
- Volúmenes persistentes para bases de datos

### Desarrollo Local (sin Docker)

- Servicios ejecutándose directamente en la máquina
- PostgreSQL local en puertos 5432, 5433, 5434
- Frontend en modo desarrollo (hot reload)

Ver [setup-local.md](./setup-local.md) para más detalles.

---

## Changelog

### 2025-02-23

- **Booking Service**: Soporta creación, listado y cancelación de reservas. Validación de conflictos (409) por solapamiento/cantidad; transición futura a "stock real" documentada.
