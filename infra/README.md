# DOMINation V2 - Infraestructura

Esta carpeta contiene la configuración de Docker Compose para levantar toda la infraestructura del proyecto.

## 🚀 Inicio Rápido

```bash
# Desde la carpeta infra/
docker-compose build --no-cache
docker-compose up -d
```

## 📦 Servicios Incluidos

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| `postgres-catalog` | 5432 | PostgreSQL para catalog-service |
| `postgres-booking` | 5433 | PostgreSQL para booking-service |
| `postgres-auth` | 5434 | PostgreSQL para auth-service |
| `auth-service` | 9000 | Microservicio de autenticación OAuth2/JWT |
| `catalog-service` | 8081 | Microservicio de catálogo |
| `booking-service` | 8082 | Microservicio de reservas |
| `gateway` | 8080 | API Gateway (punto de entrada único) |

## 🔧 Configuración

### Variables de Entorno

Los servicios están configurados con las siguientes variables de entorno:

#### Bases de Datos
- **Catalog DB**:
  - Usuario: `domination`
  - Password: `domination123`
  - Base de datos: `domination_catalog`
  - Puerto: `5432`
- **Booking DB**:
  - Usuario: `domination`
  - Password: `domination123`
  - Base de datos: `domination_booking`
  - Puerto: `5433`
- **Auth DB**:
  - Usuario: `postgres`
  - Password: `postgres`
  - Base de datos: `auth_db`
  - Puerto: `5434`

#### Servicios
- **Auth Service**: `http://localhost:9000`
- **Catalog Service**: `http://localhost:8081`
- **Booking Service**: `http://localhost:8082`
- **API Gateway**: `http://localhost:8080`

### Usuario Administrador Predeterminado

El `auth-service` crea automáticamente (mediante un DataSeeder) un usuario administrador al iniciar por primera vez:

- **Usuario**: `adminSeba`
- **Contraseña**: `123456admin`
- **Rol**: `ROLE_ADMIN` y `ROLE_USER`
- **Email**: `admin@domination.com`

**⚠️ IMPORTANTE**: Cambia esta contraseña en producción.

El usuario se crea programáticamente usando el mismo `PasswordEncoder` (BCrypt) que usa el servicio para registrar usuarios, garantizando compatibilidad total.

### Cambiar Configuración

Para cambiar configuraciones, edita el archivo `docker-compose.yml` y reinicia los servicios:

```bash
docker-compose down
docker-compose up -d
```

## 📊 Monitoreo

### Ver Logs

```bash
# Todos los servicios
docker-compose logs -f

# Un servicio específico
docker-compose logs -f catalog-service
docker-compose logs -f booking-service
docker-compose logs -f gateway
```

### Estado de los Servicios

```bash
docker-compose ps
```

### Health Checks

Verificar que los servicios estén saludables:

```bash
# Gateway
curl http://localhost:8080/actuator/health

# Catalog Service
curl http://localhost:8081/actuator/health

# Booking Service
curl http://localhost:8082/actuator/health
```

## 🗄️ Acceso a las Bases de Datos

### Catalog Database

```bash
docker exec -it domination-postgres-catalog psql -U domination -d domination_catalog
```

### Booking Database

```bash
docker exec -it domination-postgres-booking psql -U domination -d domination_booking
```

### Desde Host (usando cliente PostgreSQL local)

```bash
# Catalog DB
psql -h localhost -p 5432 -U domination -d domination_catalog

# Booking DB
psql -h localhost -p 5433 -U domination -d domination_booking
```

## 🔄 Comandos Útiles

### Detener Servicios

```bash
docker-compose stop
```

### Reiniciar Servicios

```bash
docker-compose restart
```

### Eliminar Todo (incluye volúmenes)

```bash
docker-compose down -v
```

### Rebuild de un Servicio Específico

```bash
docker-compose build --no-cache catalog-service
docker-compose up -d catalog-service
```

## 🧪 Testing

### Endpoints Públicos

```bash
# Listar sucursales
curl http://localhost:8080/api/catalog/branches

# Listar items
curl http://localhost:8080/api/catalog/items

# Item específico
curl http://localhost:8080/api/catalog/items/1
```

### Endpoints Protegidos (requieren JWT)

```bash
# Crear reserva
curl -X POST http://localhost:8080/api/booking/reservations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": 1,
    "startAt": "2026-01-15T10:00:00",
    "endAt": "2026-01-15T12:00:00",
    "lines": [
      {
        "itemId": 1,
        "quantity": 1
      }
    ]
  }'

# Mis reservas
curl http://localhost:8080/api/booking/my/reservations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🐛 Troubleshooting

### Problema: Los servicios no pueden conectarse entre sí

**Solución**: Verifica que todos los servicios estén en la misma red Docker:

```bash
docker network inspect infra_domination-network
```

### Problema: Puerto ya en uso

**Solución**: Cambia los puertos en `docker-compose.yml` o detén el proceso que está usando el puerto:

```bash
# En Windows
netstat -ano | findstr :8080

# En Linux/Mac
lsof -i :8080
```

### Problema: Base de datos no inicializa

**Solución**: Elimina los volúmenes y vuelve a crear:

```bash
docker-compose down -v
docker-compose up -d
```

### Problema: Auth Service no accesible desde contenedores

**Solución**: Asegúrate de que el Auth Service esté corriendo en el host y usa `host.docker.internal:9000` en la configuración (ya configurado en el docker-compose.yml).

## 📝 Notas

- Los volúmenes de las bases de datos persisten los datos entre reinicios
- Los servicios se rebuildan automáticamente con `docker-compose up --build`
- Para desarrollo local sin Docker, consulta el README principal del proyecto
- El frontend React NO está incluido en Docker Compose (corre con `npm run dev`)

## 🔗 Enlaces Útiles

- Swagger Catalog Service: http://localhost:8081/swagger-ui.html
- Swagger Booking Service: http://localhost:8082/swagger-ui.html
- Gateway Health: http://localhost:8080/actuator/health

