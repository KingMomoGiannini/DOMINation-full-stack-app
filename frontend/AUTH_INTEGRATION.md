# 🔐 Integración de Autenticación - DOMINation V2

## Arquitectura

La aplicación implementa un flujo de autenticación completo con OAuth2 + JWT:

```
┌─────────────┐      ┌──────────────┐      ┌─────────────────┐
│   Frontend  │─────▶│ Auth Service │◀────▶│ PostgreSQL (DB) │
│  React TS   │      │  (Port 9000) │      │   auth_db       │
└─────────────┘      └──────────────┘      └─────────────────┘
       │
       │ JWT Token
       ▼
┌─────────────┐      ┌──────────────┐
│   Gateway   │─────▶│   Catalog    │
│ (Port 8080) │      │   Service    │
└─────────────┘      │ (Port 8081)  │
       │             └──────────────┘
       │
       ▼             ┌──────────────┐
                    │   Booking    │
                    │   Service    │
                    │ (Port 8082)  │
                    └──────────────┘
```

## Flujo de Autenticación

### 1. Registro de Usuario

```typescript
POST http://localhost:9000/auth/register
{
  "username": "nuevo_usuario",
  "password": "password123",
  "email": "usuario@email.com"
}

Response:
{
  "message": "Usuario registrado exitosamente",
  "token": "eyJhbGciOiJSUz..."
}
```

**Frontend:**
- El usuario completa el formulario en `/register`
- Se llama a `register()` desde `apiClient.ts`
- El token se guarda automáticamente en `localStorage`
- El usuario es redirigido a la página principal

### 2. Login de Usuario

```typescript
POST http://localhost:9000/auth/login
{
  "username": "usuario",
  "password": "password123"
}

Response:
{
  "message": "Login exitoso",
  "token": "eyJhbGciOiJSUz..."
}
```

**Frontend:**
- El usuario completa el formulario en `/login`
- Se llama a `login()` desde `apiClient.ts`
- El token se guarda automáticamente en `localStorage`
- El contexto de autenticación se actualiza
- La navbar muestra el nombre de usuario
- El usuario puede acceder a endpoints protegidos

### 3. Uso del Token en Requests

Cuando el usuario hace una request a un endpoint protegido:

```typescript
// En apiClient.ts
const headers: HeadersInit = {
  'Content-Type': 'application/json',
};

if (requiresAuth) {
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
}
```

**Ejemplo: Crear Reserva**
```bash
POST http://localhost:8080/api/booking/reservations
Authorization: Bearer eyJhbGciOiJSUz...
Content-Type: application/json

{
  "branchId": 1,
  "startAt": "2026-01-15T10:00:00",
  "endAt": "2026-01-15T12:00:00",
  "lines": [
    {"itemId": 1, "quantity": 1}
  ]
}
```

### 4. Logout

**Frontend:**
- El usuario hace clic en "Cerrar Sesión" en la navbar
- Se llama a `logout()` desde `AuthContext`
- El token se elimina de `localStorage`
- El contexto se actualiza
- La navbar vuelve a mostrar "Iniciar Sesión"

## Contexto de Autenticación

El `AuthContext` proporciona:

```typescript
interface AuthContextType {
  isAuthenticated: boolean;    // Estado de autenticación
  username: string | null;     // Nombre del usuario actual
  login: (token, username) => void;  // Función para login
  logout: () => void;          // Función para logout
}
```

**Uso en componentes:**
```typescript
import { useAuth } from '../context/AuthContext';

function MyComponent() {
  const { isAuthenticated, username, logout } = useAuth();
  
  return (
    <div>
      {isAuthenticated ? (
        <p>Bienvenido, {username}!</p>
      ) : (
        <Link to="/login">Iniciar Sesión</Link>
      )}
    </div>
  );
}
```

## Estructura del JWT

El token JWT generado por el auth-service contiene:

```json
{
  "iss": "http://localhost:8080",
  "iat": 1704652800,
  "exp": 1704656400,
  "sub": "nombre_usuario",
  "scope": "read write openid profile",
  "authorities": "ROLE_USER ROLE_ADMIN",
  "roles": "ROLE_USER ROLE_ADMIN"
}
```

- **iss**: Issuer (emisor del token)
- **iat**: Issued At (fecha de emisión)
- **exp**: Expiration (fecha de expiración - 1 hora)
- **sub**: Subject (username del usuario)
- **authorities/roles**: Roles del usuario para autorización

## Endpoints Públicos vs Protegidos

### Públicos (No requieren token):

- `GET /api/catalog/branches` - Listar sucursales
- `GET /api/catalog/items` - Listar items
- `GET /api/catalog/items/{id}` - Detalle de item

### Protegidos (Requieren token):

- `POST /api/booking/reservations` - Crear reserva
- `GET /api/booking/my/reservations` - Mis reservas
- `POST /api/catalog/admin/branches` - Crear sucursal (ROLE_ADMIN)
- `POST /api/catalog/admin/items` - Crear item (ROLE_ADMIN)

## Manejo de Errores

### Token Expirado (401)

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token has expired"
}
```

**Frontend:** Debería redirigir al usuario a `/login`

### Token Inválido (403)

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Invalid token"
}
```

**Frontend:** Debería limpiar el token y redirigir a `/login`

### Credenciales Incorrectas (400)

```json
{
  "message": "Usuario o contraseña incorrectos"
}
```

**Frontend:** Muestra el error en el formulario

## Configuración

### Auth Service (`application.properties`)

```properties
server.port=9000
spring.security.oauth2.authorizationserver.issuer=http://localhost:9000
```

### Frontend (`.env`)

```properties
VITE_API_BASE_URL=http://localhost:8080
```

### Catalog & Booking Services

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9000
```

## Testing

### 1. Verificar Auth Service

```bash
curl http://localhost:9000/actuator/health
```

### 2. Registrar Usuario

```bash
curl -X POST http://localhost:9000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "password": "test123",
    "email": "test@test.com"
  }'
```

### 3. Login

```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "password": "test123"
  }'
```

### 4. Usar Token

```bash
TOKEN="eyJhbGciOiJSUz..."

curl -X POST http://localhost:8080/api/booking/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": 1,
    "startAt": "2026-01-15T10:00:00",
    "endAt": "2026-01-15T12:00:00",
    "lines": [{"itemId": 1, "quantity": 1}]
  }'
```

## Mejoras Futuras

- [ ] Implementar refresh tokens automáticos
- [ ] Agregar "Recordarme" en el login
- [ ] Recuperación de contraseña por email
- [ ] Verificación de email al registrarse
- [ ] Login con OAuth2 (Google, GitHub)
- [ ] 2FA (Two-Factor Authentication)
- [ ] Rate limiting en login
- [ ] Historial de sesiones


