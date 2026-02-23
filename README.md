# DOMINation V2 - Sistema de Gestión de Reservas de Salas de Ensayo

Monorepo completo con arquitectura de microservicios para la gestión de sucursales, inventario y reservas.

## 🚀 Quickstart (5 minutos)

### 1. Levantar Backend con Docker Compose

```bash
cd infra
docker-compose up -d
```

Esto levanta automáticamente:
- **3 bases de datos PostgreSQL** (puertos 5432, 5433, 5434)
- **auth-service** (puerto 9000) - Autenticación OAuth2/JWT
- **catalog-service** (puerto 8081) - Gestión de sucursales e inventario
- **booking-service** (puerto 8082) - Gestión de reservas
- **gateway** (puerto 8080) - API Gateway
- **Prometheus** (puerto 9090) - Métricas
- **Grafana** (puerto 3000) - Dashboards

### 2. Levantar Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend disponible en: **http://localhost:5173**

### 3. Verificar que todo funciona

```bash
# Health check del gateway
curl http://localhost:8080/actuator/health

# Listar sucursales (público)
curl http://localhost:8080/api/catalog/branches
```

### 4. Login

- URL: http://localhost:5173/login
- Usuario: `adminSeba`
- Contraseña: `123456admin`

**✅ Listo!** Ya podes usar la aplicación.

> 📖 Para más detalles: ver [Documentación Técnica](./docs/)

## 🏗️ Arquitectura

- **Frontend**: React 18 + TypeScript + Vite (puerto 5173)
- **API Gateway**: Spring Cloud Gateway (puerto 8080)
- **Microservicios**:
  - `auth-service` (puerto 9000) - OAuth2/JWT Authorization Server
  - `catalog-service` (puerto 8081) - Gestión de sucursales e inventario
  - `booking-service` (puerto 8082) - Gestión de reservas
- **Bases de Datos**: PostgreSQL (una DB por servicio)
- **Observabilidad**: Prometheus + Grafana
- **Seguridad**: OAuth2/JWT con Authorization Server y Resource Servers

## 📋 Requisitos Previos

- **Docker & Docker Compose** (para backend)
- **Node.js 18+** (para frontend)
- **Java 21 + Maven 3.9+** (solo si desarrollas sin Docker)

## 📡 URLs y Endpoints

### Servicios

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Frontend** | http://localhost:5173 | Aplicación React |
| **Gateway** | http://localhost:8080 | API Gateway |
| **Auth Service** | http://localhost:9000 | Autenticación OAuth2/JWT |
| **Catalog Service** | http://localhost:8081 | Gestión de catálogo |
| **Booking Service** | http://localhost:8082 | Gestión de reservas |
| **Prometheus** | http://localhost:9090 | Métricas |
| **Grafana** | http://localhost:3000 | Dashboards (admin/admin) |

### Endpoints Principales (vía Gateway)

- `GET /api/catalog/branches` - Listar sucursales (público)
- `GET /api/catalog/items` - Listar items (público)
- `POST /api/booking/reservations` - Crear reserva (requiere JWT) → retorna **201**
- `GET /api/booking/my/reservations` - Mis reservas (ROLE_USER)
- `GET /api/booking/provider/reservations` - Reservas del provider/sucursal (ROLE_PROVIDER)
- `POST /api/booking/reservations/{id}/cancel` - Cancelar reserva (idempotente, retorna 200)

### Actuator Endpoints

Todos los servicios exponen:
- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Métricas Prometheus

### Swagger UI (desarrollo)

- Catalog Service: http://localhost:8081/swagger-ui.html
- Booking Service: http://localhost:8082/swagger-ui.html (ver sección "Cómo usar Swagger con JWT" abajo)

## 🔐 Autenticación

La aplicación ahora cuenta con un sistema de autenticación completo integrado con el auth-service.

### Usuario Administrador Predeterminado:

El sistema crea automáticamente un usuario administrador al iniciar:
- **Usuario**: `adminSeba`
- **Contraseña**: `123456admin`
- **Roles**: ROLE_ADMIN y ROLE_USER

**⚠️ IMPORTANTE**: Cambia esta contraseña en producción.

El usuario se crea mediante un `DataSeeder` en el `auth-service`, usando BCrypt para hashear la contraseña correctamente.

### Login desde el Frontend:

1. Ve a `http://localhost:5173/login`
2. Ingresa las credenciales del administrador:
   - **Usuario**: `adminSeba`
   - **Contraseña**: `123456admin`
3. El sistema guardará automáticamente el token JWT
4. Podrás acceder a las funcionalidades protegidas (crear reservas, gestionar catálogo)

### Registro de Nuevos Usuarios:

1. Ve a `http://localhost:5173/register`
2. Completa el formulario con:
   - Usuario
   - Email
   - Contraseña (mínimo 6 caracteres)
3. Automáticamente iniciarás sesión tras el registro

### Endpoints del Auth Service:

```bash
# Registro
curl -X POST http://localhost:9000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "usuario_nuevo",
    "password": "password123",
    "email": "usuario@email.com"
  }'

# Login
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "usuario",
    "password": "password123"
  }'
```

### Testing de Endpoints Protegidos:

```bash
# Público - Listar sucursales
curl http://localhost:8080/api/catalog/branches

# Público - Listar items
curl http://localhost:8080/api/catalog/items

# Privado - Crear reserva (requiere token)
curl -X POST http://localhost:8080/api/booking/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "branchId": 1,
    "startAt": "2026-01-15T10:00:00",
    "endAt": "2026-01-15T12:00:00",
    "lines": [
      {"itemId": 1, "quantity": 1}
    ]
  }'

# Privado - Mis reservas
curl http://localhost:8080/api/booking/my/reservations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📚 Documentación Técnica

- [**Arquitectura**](./docs/arquitectura.md) - Diseño del sistema y componentes
- [**Setup Local**](./docs/setup-local.md) - Desarrollo sin Docker
- [**Docker Compose**](./docs/docker-compose.md) - Configuración de contenedores
- [**Observabilidad**](./docs/observabilidad.md) - Prometheus, Grafana y métricas
- [**Troubleshooting**](./docs/troubleshooting.md) - Solución de problemas comunes

## 🛠️ Desarrollo Local (sin Docker)

Ver [docs/setup-local.md](./docs/setup-local.md) para instrucciones detalladas.

## 🗄️ Base de Datos

Las bases de datos se inicializan automáticamente con Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

### Datos de Prueba

El `catalog-service` incluye un seeder que crea:
- 2 sucursales (Buenos Aires Centro, Belgrano)
- 4 items de ejemplo (salas, instrumentos)

## 📦 Compilación

### Backend (cada servicio)

```bash
cd services/catalog-service
mvn clean package

cd ../booking-service
mvn clean package

cd ../../gateway
mvn clean package
```

### Frontend

```bash
cd frontend
npm run build
```

## 🌐 Variables de Entorno

### Frontend (.env)

```
VITE_API_BASE_URL=http://localhost:8080
```

### Backend (application.properties)

Cada servicio tiene su configuración en `src/main/resources/application.properties`.

## 📚 Documentación API

Una vez levantados los servicios:

- Catalog Service: http://localhost:8081/swagger-ui.html
- Booking Service: http://localhost:8082/swagger-ui.html

> **Nota**: El auth-service **no** expone Swagger; el login se hace por frontend o por curl/Postman.

## Cómo usar Swagger con JWT (booking-service)

El booking-service expone Swagger UI en http://localhost:8082/swagger-ui.html. Para probar endpoints protegidos:

1. **Obtener token JWT** (elige una opción):
   - **Frontend**: Login en http://localhost:5173/login → DevTools → Application → Local Storage → copiar el token.
   - **Postman/curl**:
     ```bash
     curl -X POST http://localhost:9000/auth/login \
       -H "Content-Type: application/json" \
       -d '{"username":"adminSeba","password":"123456admin"}'
     # Para ROLE_PROVIDER: usar usuario providerDemo (creado por seeder si aplica)
     ```
     Respuesta incluye `accessToken`.

2. **En Swagger**: Ir a http://localhost:8082/swagger-ui.html → **Authorize** (candado) → pegar solo el token (sin "Bearer ") → Authorize.

3. **Ejecutar endpoints**:
   - `GET /api/booking/my/reservations` → 200
   - `GET /api/booking/provider/reservations` → 200 (si tienes ROLE_PROVIDER)
   - `POST /api/booking/reservations` → 201 al crear
   - `POST /api/booking/reservations/{id}/cancel` → 200 (idempotente)

## 🔄 Flujo de Trabajo

1. **Navegación pública**: Los usuarios pueden ver sucursales e items sin autenticarse
2. **Login**: El usuario se autentica contra el Auth Service externo
3. **Reserva**: Con el JWT, el usuario crea reservas
4. **Validación**: El booking-service valida disponibilidad consultando catalog-service

## 🧪 Testing

### Test de Endpoints Públicos

```bash
# Healthcheck
curl http://localhost:8080/actuator/health

# Branches
curl http://localhost:8080/api/catalog/branches

# Items
curl http://localhost:8080/api/catalog/items
```

## 📖 Estructura del Proyecto

```
/
├── frontend/              - React 18 + TypeScript + Vite
├── gateway/               - Spring Cloud Gateway
├── services/
│   ├── auth-service/      - OAuth2/JWT Authorization Server
│   ├── catalog-service/   - Gestión de sucursales e inventario
│   └── booking-service/   - Gestión de reservas
├── infra/                 - Docker Compose y configs
│   ├── docker-compose.yml - Orquestación de servicios
│   └── prometheus/        - Configuración de Prometheus
└── docs/                  - Documentación técnica
```

## 🎨 Diseño y UX

La aplicación cuenta con:

- ✅ Diseño moderno con gradientes y glassmorphism
- ✅ Navbar sticky con información del usuario
- ✅ Sistema de autenticación visual integrado
- ✅ Formularios con validación y feedback
- ✅ Animaciones suaves y transiciones
- ✅ Responsive design para móviles
- ✅ Paleta de colores consistente
- ✅ Iconos y badges informativos

## 🔒 Seguridad

- **OAuth2 + JWT** para autenticación
- **Resource Servers** en catalog y booking services
- **Tokens** con expiración de 1 hora
- **Refresh tokens** con 7 días de validez
- **CORS** configurado en gateway para frontend (localhost:5173)
- **Request-Id tracing** para correlación de logs

## 🔍 Observabilidad

- **Prometheus** scraping métricas de todos los servicios
- **Grafana** con dashboards preconfigurados
- **Request-Id** propagado en headers y logs (MDC)
- **Actuator** endpoints en todos los servicios

Ver [docs/observabilidad.md](./docs/observabilidad.md) para más detalles.

## 🤝 Contribución

Este es un proyecto académico/profesional. Para contribuir:

1. Fork el repositorio
2. Crea una rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

## 📝 Licencia

Proyecto desarrollado por Sebastián Giannini - INSPT

---

## Changelog de documentación

### 2025-02-23

- **Swagger en booking-service**: Configuración OpenAPI con SecurityScheme Bearer JWT; Swagger UI carga correctamente tras permitir `/api-docs` y `/swagger-ui/**` en SecurityConfig.
- **Endpoints booking-service**: Agregados `POST /api/booking/reservations/{id}/cancel`, `GET /api/booking/provider/reservations`; convención 201 para crear reserva.
- **Conflictos 409**: Al crear reserva en horario ya ocupado, retorna 409 con body estructurado (title, detail, status, instance, timestamp). Validación actual: lógica (solapamiento/cantidad reservada); "stock real" pendiente.
- **Testing booking-service**: Tests unitarios (ReservationService), integración con Postgres local; profile `test` con `application-test.properties`; propiedades clave documentadas en [setup-local.md](./docs/setup-local.md) y [docker-compose.md](./docs/docker-compose.md).

## 🔮 Roadmap

- [ ] Internacionalización (i18n) Español/English
- [ ] Sistema de notificaciones en tiempo real
- [ ] Panel de administración completo con dashboard
- [ ] Reportes y analytics con gráficos
- [ ] Pasarela de pagos integrada
- [ ] App móvil con React Native
- [ ] Sistema de calificaciones y reviews
- [ ] Chat en vivo para soporte

