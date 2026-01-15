# 🔐 Implementación de Roles y Claims en JWT

## 📋 ARCHIVOS MODIFICADOS

1. ✅ **RoleName.java** - Agregado `ROLE_PROVIDER`
2. ✅ **DataSeeder.java** - Creación de rol PROVIDER y usuario demo
3. ✅ **OAuth2TokenService.java** - Authorities como List (formato correcto para Resource Servers)

---

## 🔧 CAMBIOS REALIZADOS

### 1. RoleName.java
```java
public enum RoleName {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_PROVIDER  // ← NUEVO
}
```

### 2. DataSeeder.java
- ✅ Agregado seed de `ROLE_PROVIDER` en `seedRoles()`
- ✅ Nuevo método `seedProviderUser()` que crea usuario demo:
  - **Username**: `providerDemo`
  - **Password**: `provider123`
  - **Email**: `provider@domination.com`
  - **Roles**: `ROLE_PROVIDER` + `ROLE_USER`

### 3. OAuth2TokenService.java
- ✅ Cambio de `String authorities` a `List<String> authorities`
- ✅ Claim "authorities" ahora es array: `["ROLE_ADMIN", "ROLE_USER"]`
- ✅ Agregado claim "userId" para auditoría
- ❌ Removido claim "roles" (redundante)

---

## 🚀 REBUILD Y DEPLOY

```bash
# 1. Detener auth-service
cd infra
docker-compose stop auth-service

# 2. IMPORTANTE: Borrar volumen de DB para recrear roles
docker volume rm infra_auth-db-data

# 3. Rebuild auth-service
cd ../services/auth-service
mvn clean package -DskipTests

# 4. Rebuild imagen Docker
cd ../../infra
docker-compose build auth-service

# 5. Levantar todos los servicios
docker-compose up -d

# 6. Ver logs para verificar creación de roles y usuarios
docker-compose logs -f auth-service
```

**Deberías ver:**
```
✅ Rol ROLE_ADMIN creado
✅ Rol ROLE_USER creado
✅ Rol ROLE_PROVIDER creado
✅ Usuario administrador creado:
   👤 Username: adminSeba
✅ Usuario provider demo creado:
   👤 Username: providerDemo
   🎭 Roles: ROLE_PROVIDER, ROLE_USER
```

---

## 🧪 TESTING

### 1. Login como Admin
```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "adminSeba",
    "password": "123456admin"
  }'
```

**Respuesta esperada:**
```json
{
  "message": "Login exitoso.",
  "token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

### 2. Login como Provider
```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "providerDemo",
    "password": "provider123"
  }'
```

### 3. Decodificar JWT

Ve a **https://jwt.io** y pega el token. Deberías ver:

**Header:**
```json
{
  "alg": "RS256"
}
```

**Payload (adminSeba):**
```json
{
  "iss": "http://localhost:8080",
  "sub": "adminSeba",
  "exp": 1735497600,
  "iat": 1735494000,
  "scope": "read write openid profile",
  "authorities": [
    "ROLE_ADMIN",
    "ROLE_USER"
  ],
  "userId": 1
}
```

**Payload (providerDemo):**
```json
{
  "iss": "http://localhost:8080",
  "sub": "providerDemo",
  "exp": 1735497600,
  "iat": 1735494000,
  "scope": "read write openid profile",
  "authorities": [
    "ROLE_PROVIDER",
    "ROLE_USER"
  ],
  "userId": 2
}
```

### 4. Test en Catalog-Service

```bash
# 1. Login como provider
TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"providerDemo","password":"provider123"}' \
  | jq -r '.token')

# 2. Crear sucursal (requiere ROLE_PROVIDER)
curl -X POST http://localhost:8080/api/catalog/provider/branches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Sede Provider Demo",
    "address": "Calle Test 123"
  }'
```

**Respuesta esperada:** `201 Created` con el branch creado.

### 5. Test @PreAuthorize

En catalog-service, el `ProviderController` tiene:
```java
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderController {
    // ...
}
```

Si intentas acceder con un usuario que NO tiene `ROLE_PROVIDER`, recibirás:
```json
{
  "error": "Access Denied",
  "message": "Forbidden",
  "status": 403
}
```

---

## ✅ VERIFICACIÓN DE CLAIMS EN RESOURCE SERVERS

### catalog-service / booking-service

El `SecurityConfig` ya está configurado para leer authorities:

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
    grantedAuthoritiesConverter.setAuthorityPrefix("");  // Ya viene con ROLE_
    
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
}
```

**Cómo funciona:**
1. El JWT llega con claim `"authorities": ["ROLE_PROVIDER", "ROLE_USER"]`
2. `JwtGrantedAuthoritiesConverter` lo convierte a `GrantedAuthority`
3. `@PreAuthorize("hasRole('PROVIDER')")` valida si existe "ROLE_PROVIDER"
4. Spring Security quita automáticamente el prefijo "ROLE_" al evaluar `hasRole()`

---

## 🔍 TROUBLESHOOTING

### Error: "ROLE_PROVIDER no está configurado en la base"

**Causa:** El DataSeeder no se ejecutó o la DB no se recreó.

**Solución:**
```bash
docker-compose down
docker volume rm infra_auth-db-data
docker-compose up -d
```

### Error: "authorities claim is null"

**Causa:** El JWT se generó antes de actualizar `OAuth2TokenService`.

**Solución:** Hacer login nuevamente para obtener un JWT nuevo.

### Error: 403 Forbidden en endpoints de provider

**Causa:** El usuario no tiene `ROLE_PROVIDER` o el JWT no tiene el claim correcto.

**Solución:**
1. Verificar roles en DB: `SELECT * FROM user_roles WHERE user_id = 2;`
2. Decodificar JWT en jwt.io y verificar claim "authorities"
3. Verificar que el `SecurityConfig` del Resource Server tenga el `JwtAuthenticationConverter` correcto

### JWT no tiene claim "authorities"

**Causa:** Versión vieja de `OAuth2TokenService` en ejecución.

**Solución:**
```bash
cd services/auth-service
mvn clean package -DskipTests
cd ../../infra
docker-compose build auth-service
docker-compose restart auth-service
```

---

## 📊 ESTRUCTURA FINAL DE ROLES

| Usuario | Roles | Permisos |
|---------|-------|----------|
| `adminSeba` | `ROLE_ADMIN`, `ROLE_USER` | Acceso total (admin + user) |
| `providerDemo` | `ROLE_PROVIDER`, `ROLE_USER` | Gestión de sucursales propias + reservas |
| Usuarios registrados | `ROLE_USER` | Crear reservas propias |

---

## 🎯 PRÓXIMOS PASOS

1. ✅ **COMPLETADO**: Roles en JWT
2. ⏭️ **SIGUIENTE**: Implementar `providerId` en JWT (claim custom)
3. ⏭️ **SIGUIENTE**: Validar ownership en catalog-service y booking-service
4. ⏭️ **SIGUIENTE**: Endpoints de provider y admin en ambos servicios

Ver `ARQUITECTURA_ROLES_Y_PERMISOS.md` para detalles completos.

---

**Última Actualización:** 2026-01-08  
**Estado:** ✅ IMPLEMENTADO Y LISTO PARA TESTING

