# Setup Local (sin Docker)

Esta guía explica cómo ejecutar DOMINation en modo desarrollo local, sin usar Docker Compose.

## Requisitos Previos

- **Java 21** (JDK)
- **Maven 3.9+**
- **Node.js 18+** y npm
- **PostgreSQL 15+** instalado localmente

## Configuración de Bases de Datos

### 1. Crear Bases de Datos

```sql
-- Conectar a PostgreSQL como superusuario
psql -U postgres

-- Crear base de datos para auth-service
CREATE DATABASE auth_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;

-- Crear base de datos para catalog-service
CREATE DATABASE domination_catalog;
CREATE USER domination WITH PASSWORD 'domination123';
GRANT ALL PRIVILEGES ON DATABASE domination_catalog TO domination;

-- Crear base de datos para booking-service
CREATE DATABASE domination_booking;
GRANT ALL PRIVILEGES ON DATABASE domination_booking TO domination;
```

### 2. Verificar Puertos

Asegúrate de que PostgreSQL esté escuchando en:
- Puerto **5432** (catalog-service)
- Puerto **5433** (booking-service) - configurar en `postgresql.conf`
- Puerto **5434** (auth-service) - configurar en `postgresql.conf`

O modifica los `application.properties` de cada servicio para usar el mismo puerto con diferentes bases de datos.

## Configuración de Servicios

### Auth Service

**Archivo**: `services/auth-service/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5434/auth_db
spring.datasource.username=postgres
spring.datasource.password=postgres
app.security.issuer-uri=http://localhost:9000
```

**Ejecutar**:
```bash
cd services/auth-service
mvn spring-boot:run
```

**Verificar**: http://localhost:9000/actuator/health

### Catalog Service

**Archivo**: `services/catalog-service/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/domination_catalog
spring.datasource.username=domination
spring.datasource.password=domination123
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000
```

**Ejecutar**:
```bash
cd services/catalog-service
mvn spring-boot:run
```

**Verificar**: http://localhost:8081/actuator/health

### Booking Service

**Archivo**: `services/booking-service/src/main/resources/application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/domination_booking
spring.datasource.username=domination
spring.datasource.password=domination123
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000
catalog.service.url=http://localhost:8081
```

**Ejecutar**:
```bash
cd services/booking-service
mvn spring-boot:run
```

**Verificar**: http://localhost:8082/actuator/health

### Gateway

**Archivo**: `gateway/src/main/resources/application.properties`

Las rutas ya están configuradas para apuntar a `localhost`:
- `http://catalog-service:8081` → cambiar a `http://localhost:8081`
- `http://booking-service:8082` → cambiar a `http://localhost:8082`

**Modificar** `gateway/src/main/resources/application.properties`:

```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[1].uri=http://localhost:8082
```

**Ejecutar**:
```bash
cd gateway
mvn spring-boot:run
```

**Verificar**: http://localhost:8080/actuator/health

### Frontend

**Archivo**: `frontend/.env` (crear si no existe)

```env
VITE_API_BASE_URL=http://localhost:8080
```

**Ejecutar**:
```bash
cd frontend
npm install
npm run dev
```

**Verificar**: http://localhost:5173

## Orden de Inicio Recomendado

1. **PostgreSQL** (debe estar corriendo)
2. **Auth Service** (puerto 9000)
3. **Catalog Service** (puerto 8081)
4. **Booking Service** (puerto 8082)
5. **Gateway** (puerto 8080)
6. **Frontend** (puerto 5173)

## Scripts de Inicio Rápido

### Windows (PowerShell)

Crea `start-local.ps1`:

```powershell
# Terminal 1 - Auth Service
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/auth-service; mvn spring-boot:run"

# Terminal 2 - Catalog Service
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/catalog-service; mvn spring-boot:run"

# Terminal 3 - Booking Service
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services/booking-service; mvn spring-boot:run"

# Terminal 4 - Gateway
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd gateway; mvn spring-boot:run"

# Terminal 5 - Frontend
Start-Sleep -Seconds 10
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd frontend; npm run dev"
```

### Linux/Mac (Bash)

Crea `start-local.sh`:

```bash
#!/bin/bash

# Terminal 1 - Auth Service
gnome-terminal -- bash -c "cd services/auth-service && mvn spring-boot:run; exec bash"

# Terminal 2 - Catalog Service
sleep 10
gnome-terminal -- bash -c "cd services/catalog-service && mvn spring-boot:run; exec bash"

# Terminal 3 - Booking Service
sleep 10
gnome-terminal -- bash -c "cd services/booking-service && mvn spring-boot:run; exec bash"

# Terminal 4 - Gateway
sleep 10
gnome-terminal -- bash -c "cd gateway && mvn spring-boot:run; exec bash"

# Terminal 5 - Frontend
sleep 10
gnome-terminal -- bash -c "cd frontend && npm run dev; exec bash"
```

## Verificación

Una vez que todos los servicios estén corriendo:

```bash
# Health checks
curl http://localhost:9000/actuator/health  # auth-service
curl http://localhost:8081/actuator/health  # catalog-service
curl http://localhost:8082/actuator/health  # booking-service
curl http://localhost:8080/actuator/health   # gateway

# Endpoint público
curl http://localhost:8080/api/catalog/branches
```

## Troubleshooting

### Puerto ya en uso

Si un puerto está ocupado:

1. Identifica el proceso: `netstat -ano | findstr :8080` (Windows) o `lsof -i :8080` (Linux/Mac)
2. Mata el proceso o cambia el puerto en `application.properties`

### Error de conexión a base de datos

- Verifica que PostgreSQL esté corriendo
- Verifica credenciales en `application.properties`
- Verifica que las bases de datos existan

### Error de validación JWT

- Verifica que `auth-service` esté corriendo
- Verifica que `issuer-uri` apunte a `http://localhost:9000`
- Verifica que el token sea válido (no expirado)

### CORS errors en frontend

- Verifica que el gateway esté corriendo
- Verifica que `VITE_API_BASE_URL=http://localhost:8080` en `.env`
- Verifica la configuración CORS en `gateway/application.properties`

## Ventajas vs Docker Compose

**Ventajas**:
- Hot reload más rápido
- Debugging más fácil (attach debugger)
- No requiere Docker

**Desventajas**:
- Requiere configurar PostgreSQL manualmente
- Más pasos de setup
- Dependencias del sistema operativo

## Swagger en booking-service

- **URL**: http://localhost:8082/swagger-ui.html
- **OpenAPI**: `springdoc.api-docs.path=/api-docs` (no `/v3/api-docs` por defecto)
- Si Swagger muestra "Failed to load remote configuration", suele ser porque `/api-docs` devuelve 401: debe estar permitido en `SecurityConfig` del booking-service (`/api-docs`, `/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`). Ver [troubleshooting](./troubleshooting.md).

## Tests del booking-service

Los tests usan profile `test` con `application-test.properties` apuntando a `domination_booking_test` en puerto 5433. Propiedades clave:

- `spring.test.database.replace=none` — evita que Spring reemplace el datasource por H2 embebido
- `spring.jpa.hibernate.ddl-auto=create-drop` — limpia el schema por corrida
- `spring.sql.init.mode=never`

Para correr tests solo hace falta tener **postgres-booking** levantado (puerto 5433). Ver [docker-compose.md](./docker-compose.md).

**Validación en tests**: Si se desactiva con `jakarta.persistence.validation.mode=none`, los tests evitan errores por datos incompletos, pero se recomienda usar datos válidos (ej. `price != null`) cuando sea posible.

## Recomendación

Para desarrollo diario, usa **Docker Compose** (más simple). Usa setup local solo si necesitas:
- Debugging avanzado con breakpoints
- Modificar código Java con hot reload
- Testing de integración local

---

## Changelog

### 2025-02-23

- Swagger booking-service: notas sobre `/api-docs` y SecurityConfig.
- Tests booking-service: propiedades del profile `test` y recomendaciones de validación.
