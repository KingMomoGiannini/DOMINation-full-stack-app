# Docker Compose - Configuración y Uso

Esta guía explica la configuración de Docker Compose para DOMINation y cómo usarla.

## Archivo Principal

**Ubicación**: `infra/docker-compose.yml`

## Servicios Definidos

### Bases de Datos PostgreSQL

#### postgres-catalog
- **Puerto**: 5432
- **Base de datos**: `domination_catalog`
- **Usuario**: `domination`
- **Contraseña**: `domination123`
- **Volumen**: `catalog-db-data`

#### postgres-booking
- **Puerto**: 5433
- **Base de datos**: `domination_booking`
- **Usuario**: `domination`
- **Contraseña**: `domination123`
- **Volumen**: `booking-db-data`

#### postgres-auth
- **Puerto**: 5434
- **Base de datos**: `auth_db`
- **Usuario**: `postgres`
- **Contraseña**: `postgres`
- **Volumen**: `auth-db-data`

### Servicios de Aplicación

#### auth-service
- **Puerto**: 9000
- **Build**: `../services/auth-service`
- **Dependencias**: `postgres-auth` (healthy)
- **Variables de entorno**:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-auth:5432/auth_db`
  - `ISSUER_URI=http://auth-service:9000`

#### catalog-service
- **Puerto**: 8081
- **Build**: `../services/catalog-service`
- **Dependencias**: `postgres-catalog` (healthy), `auth-service` (started)
- **Variables de entorno**:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-catalog:5432/domination_catalog`
  - `ISSUER_URI=http://auth-service:9000`

#### booking-service
- **Puerto**: 8082
- **Build**: `../services/booking-service`
- **Dependencias**: `postgres-booking` (healthy), `catalog-service` (started), `auth-service` (started)
- **Variables de entorno**:
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-booking:5432/domination_booking`
  - `ISSUER_URI=http://auth-service:9000`
  - `CATALOG_SERVICE_URL=http://catalog-service:8081`

#### gateway
- **Puerto**: 8080
- **Build**: `../gateway`
- **Dependencias**: `catalog-service` (started), `booking-service` (started)

### Observabilidad

#### prometheus
- **Puerto**: 9090
- **Imagen**: `prom/prometheus:latest`
- **Configuración**: `./prometheus/prometheus.yml`

#### grafana
- **Puerto**: 3000
- **Imagen**: `grafana/grafana:latest`
- **Dependencias**: `prometheus`

## Comandos Principales

### Levantar todos los servicios

```bash
cd infra
docker-compose up -d
```

El flag `-d` ejecuta en modo detached (background).

### Ver logs

```bash
# Todos los servicios
docker-compose logs -f

# Servicio específico
docker-compose logs -f gateway
docker-compose logs -f auth-service
```

### Detener servicios

```bash
# Detener sin eliminar contenedores
docker-compose stop

# Detener y eliminar contenedores
docker-compose down

# Detener y eliminar contenedores + volúmenes (⚠️ elimina datos)
docker-compose down -v
```

### Reconstruir imágenes

```bash
# Reconstruir todas las imágenes
docker-compose build --no-cache

# Reconstruir servicio específico
docker-compose build --no-cache gateway

# Reconstruir y levantar
docker-compose up -d --build
```

### Reiniciar un servicio

```bash
docker-compose restart gateway
docker-compose restart auth-service
```

### Ver estado de servicios

```bash
docker-compose ps
```

### Ejecutar comandos en un contenedor

```bash
# Shell en el contenedor
docker-compose exec gateway sh

# Ejecutar comando específico
docker-compose exec postgres-catalog psql -U domination -d domination_catalog
```

## Health Checks

Los servicios de PostgreSQL tienen health checks configurados:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U domination -d domination_catalog"]
  interval: 10s
  timeout: 5s
  retries: 5
```

Los servicios de aplicación esperan a que las bases de datos estén `healthy` antes de iniciar.

## Red Docker

Todos los servicios se comunican a través de la red `domination-network` (bridge).

**Nombres de host internos**:
- `postgres-catalog`
- `postgres-booking`
- `postgres-auth`
- `auth-service`
- `catalog-service`
- `booking-service`
- `gateway`
- `prometheus`
- `grafana`

## Volúmenes Persistentes

Las bases de datos usan volúmenes nombrados para persistencia:

- `catalog-db-data`
- `booking-db-data`
- `auth-db-data`

**Eliminar volúmenes**:
```bash
docker-compose down -v
```

**Backup de base de datos**:
```bash
docker-compose exec postgres-catalog pg_dump -U domination domination_catalog > backup.sql
```

**Restaurar base de datos**:
```bash
docker-compose exec -T postgres-catalog psql -U domination domination_catalog < backup.sql
```

## Variables de Entorno

Las variables de entorno se pueden sobrescribir con un archivo `.env` en `infra/`:

```env
# .env
POSTGRES_PASSWORD=mi_password_seguro
ISSUER_URI=http://auth-service:9000
```

O pasarlas directamente:

```bash
ISSUER_URI=http://localhost:9000 docker-compose up -d
```

## Orden de Inicio

Docker Compose respeta las dependencias definidas:

1. **PostgreSQL** (health checks)
2. **auth-service** (espera postgres-auth)
3. **catalog-service** (espera postgres-catalog + auth-service)
4. **booking-service** (espera postgres-booking + catalog-service + auth-service)
5. **gateway** (espera catalog-service + booking-service)
6. **prometheus** y **grafana** (independientes)

## Troubleshooting

### Servicio no inicia

```bash
# Ver logs del servicio
docker-compose logs service-name

# Ver estado
docker-compose ps

# Reiniciar
docker-compose restart service-name
```

### Puerto ya en uso

Si un puerto está ocupado, modifica el mapeo en `docker-compose.yml`:

```yaml
ports:
  - "8080:8080"  # Cambiar 8080 por otro puerto
```

### Limpiar todo y empezar de nuevo

```bash
# Detener y eliminar contenedores, redes y volúmenes
docker-compose down -v

# Eliminar imágenes
docker-compose rm -f

# Reconstruir desde cero
docker-compose build --no-cache
docker-compose up -d
```

### Ver uso de recursos

```bash
docker stats
```

### Acceder a base de datos desde host

```bash
# Desde el host
psql -h localhost -p 5432 -U domination -d domination_catalog
# Password: domination123
```

## Configuración de Prometheus

**Archivo**: `infra/prometheus/prometheus.yml`

```yaml
scrape_configs:
  - job_name: gateway
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['domination-gateway:8080']
  # ... más targets
```

Los targets usan nombres de host internos de Docker.

## Configuración de Grafana

1. Acceder a http://localhost:3000
2. Login: `admin` / `admin`
3. Agregar datasource:
   - Tipo: Prometheus
   - URL: `http://prometheus:9090`
4. Importar dashboard (ver [observabilidad.md](./observabilidad.md))

## Tests del booking-service

Para ejecutar los tests del booking-service solo hace falta levantar **postgres-booking**:

```bash
cd infra
docker-compose up -d postgres-booking
```

Crear la base de datos de test en el puerto 5433:

```bash
docker-compose exec postgres-booking psql -U domination -d domination_booking -c "CREATE DATABASE domination_booking_test;"
```

**Nota:** Si el usuario `domination` no tiene permiso para crear bases de datos, usar como fallback:

```bash
# Conectar como postgres (superuser) y dar permiso
docker-compose exec postgres-booking psql -U postgres -d postgres -c "ALTER USER domination CREATEDB;"

# Luego crear la base de datos
docker-compose exec postgres-booking psql -U domination -d domination_booking -c "CREATE DATABASE domination_booking_test;"
```

Los tests usan profile `test` con `application-test.properties` (ver [setup-local.md](./setup-local.md)).

## Mejores Prácticas

1. **No commitees datos sensibles**: Usar `.env` para contraseñas
2. **Usa health checks**: Aseguran que los servicios estén listos
3. **Volúmenes nombrados**: Facilitan backup y restauración
4. **Logs estructurados**: Facilita debugging
5. **Resource limits**: En producción, define límites de CPU/memoria

---

## Changelog

### 2026-02-23

- Nota sobre tests de booking-service: solo requiere postgres-booking (puerto 5433).

## Producción

Para producción, considerar:

- Usar Docker Swarm o Kubernetes
- Secrets management (Docker Secrets, Vault)
- Health checks más estrictos
- Resource limits
- Logging centralizado (ELK, Loki)
- Monitoring avanzado (Alertmanager)
