# DOMINation V2 - Sistema de Gestión de Reservas de Salas de Ensayo

Monorepo completo con arquitectura de microservicios para la gestión de sucursales, inventario y reservas.

## 🏗️ Arquitectura

- **Frontend**: React 18 + TypeScript + Vite
- **API Gateway**: Spring Cloud Gateway
- **Microservicios**:
  - `auth-service`: Autenticación OAuth2/JWT (Authorization Server)
  - `catalog-service`: Gestión de sucursales, items e inventario
  - `booking-service`: Gestión de reservas
- **Bases de Datos**: PostgreSQL (una DB por servicio)
- **Seguridad**: OAuth2/JWT con Authorization Server y Resource Servers

## 📋 Requisitos Previos

- Java 21
- Maven 3.9+
- Node.js 18+
- Docker & Docker Compose

## 🚀 Inicio Rápido

### 1. Levantar Infraestructura con Docker Compose

```bash
cd infra
docker-compose build --no-cache
docker-compose up -d
```

Esto levantará:
- PostgreSQL para auth-service (puerto 5434)
- PostgreSQL para catalog-service (puerto 5432)
- PostgreSQL para booking-service (puerto 5433)
- auth-service (puerto 9000)
- catalog-service (puerto 8081)
- booking-service (puerto 8082)
- gateway (puerto 8080)

### 2. Levantar Frontend (Desarrollo)

```bash
cd frontend
npm install
npm run dev
```

El frontend estará disponible en `http://localhost:5173`

## 📡 Endpoints Principales

### Gateway (puerto 8080)

- `GET /api/catalog/branches` - Listar sucursales (público)
- `GET /api/catalog/items` - Listar items (público)
- `POST /api/booking/reservations` - Crear reserva (requiere JWT)
- `GET /api/booking/my/reservations` - Mis reservas (requiere JWT)

### Acceso Directo a Servicios (desarrollo)

- Catalog Service: `http://localhost:8081`
  - Swagger UI: `http://localhost:8081/swagger-ui.html`
- Booking Service: `http://localhost:8082`
  - Swagger UI: `http://localhost:8082/swagger-ui.html`

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

## 🛠️ Desarrollo Local (sin Docker)

### Backend Services

```bash
# Terminal 1 - Catalog Service
cd services/catalog-service
mvn spring-boot:run

# Terminal 2 - Booking Service
cd services/booking-service
mvn spring-boot:run

# Terminal 3 - Gateway
cd gateway
mvn spring-boot:run
```

**Nota**: Necesitarás PostgreSQL corriendo localmente en puertos 5432 y 5433.

### Frontend

```bash
cd frontend
npm run dev
```

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
├── frontend/          - React TS aplicación
├── gateway/           - Spring Cloud Gateway
├── services/
│   ├── catalog-service/   - Microservicio de catálogo
│   └── booking-service/   - Microservicio de reservas
└── infra/             - Docker Compose y configs
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

- OAuth2 + JWT para autenticación
- Resource Servers en catalog y booking services
- Tokens con expiración de 1 hora
- Refresh tokens con 7 días de validez
- Endpoints públicos para navegación
- Endpoints protegidos para reservas
- CORS configurado para el frontend

## 🤝 Contribución

Este es un proyecto académico/profesional. Para contribuir:

1. Fork el repositorio
2. Crea una rama feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -m 'Agregar nueva funcionalidad'`)
4. Push a la rama (`git push origin feature/nueva-funcionalidad`)
5. Abre un Pull Request

## 📝 Licencia

Proyecto desarrollado por Sebastián Giannini - INSPT

## 🔮 Roadmap

- [ ] Internacionalización (i18n) Español/English
- [ ] Sistema de notificaciones en tiempo real
- [ ] Panel de administración completo con dashboard
- [ ] Reportes y analytics con gráficos
- [ ] Pasarela de pagos integrada
- [ ] App móvil con React Native
- [ ] Sistema de calificaciones y reviews
- [ ] Chat en vivo para soporte

