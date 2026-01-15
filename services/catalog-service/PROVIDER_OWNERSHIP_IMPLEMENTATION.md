# 🔐 Implementación de Ownership para Providers

## 📋 RESUMEN

**Objetivo:** Implementar control de acceso para providers usando `jwt.getClaim("userId")` como `providerId`.

**Decisión Arquitectónica:** NO emitir claim `providerId` separado. Usar `userId` directamente.

---

## 📄 ARCHIVOS MODIFICADOS

### 1. ✅ Branch.java
- **Cambio:** Agregado campo `providerId` (Long)
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/domain/Branch.java`

```java
@Column(name = "provider_id")
private Long providerId;
```

### 2. ✅ BranchRepository.java
- **Cambio:** Agregado método `findByProviderId(Long providerId)`
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/repository/BranchRepository.java`

```java
List<Branch> findByProviderId(Long providerId);
```

### 3. ✅ BranchDTO.java
- **Cambio:** Agregado campo `providerId`
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/dto/BranchDTO.java`

### 4. ✅ BranchMapper.java
- **Cambio:** Mapeo de `providerId` en ambas direcciones
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/mapper/BranchMapper.java`

### 5. ✅ BranchService.java
- **Cambios:** Agregados 4 métodos para provider:
  - `findByProviderId(Long providerId)`
  - `createForProvider(CreateBranchRequest, Long providerId)`
  - `updateForProvider(Long id, CreateBranchRequest, Long providerId)`
  - `deleteForProvider(Long id, Long providerId)`
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/service/BranchService.java`

### 6. ✅ ProviderController.java (NUEVO)
- **Endpoints:**
  - `GET /api/catalog/provider/branches` - Ver mis sucursales
  - `POST /api/catalog/provider/branches` - Crear sucursal
  - `PUT /api/catalog/provider/branches/{id}` - Editar sucursal propia
  - `DELETE /api/catalog/provider/branches/{id}` - Eliminar sucursal propia
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/controller/ProviderController.java`

### 7. ✅ SecurityConfig.java
- **Estado:** Ya tenía `@EnableMethodSecurity` ✓
- **No requiere cambios**

### 8. ✅ DataSeeder.java
- **Cambio:** Branches de ejemplo asignadas a `providerId=2` (providerDemo)
- **Path:** `services/catalog-service/src/main/java/com/domination/catalog/config/DataSeeder.java`

---

## 🗄️ MIGRACIÓN DE BASE DE DATOS

### Opción A: DDL Auto (Desarrollo)

En `application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update
```

Al reiniciar, Hibernate creará automáticamente la columna `provider_id`.

### Opción B: SQL Manual (Producción)

```sql
-- Agregar columna provider_id a branches
ALTER TABLE branches ADD COLUMN provider_id BIGINT;

-- Actualizar datos existentes (asignar a providerDemo)
UPDATE branches SET provider_id = 2 WHERE provider_id IS NULL;

-- Verificar
SELECT id, name, provider_id FROM branches;
```

### Opción C: Recrear Base de Datos (Dev)

```bash
cd infra
docker-compose stop catalog-service
docker volume rm infra_catalog-db-data
docker-compose up -d catalog-service
```

---

## 🚀 REBUILD Y DEPLOY

```bash
# 1. Compilar catalog-service
cd services/catalog-service
mvn clean package -DskipTests

# 2. Rebuild Docker
cd ../../infra
docker-compose build catalog-service

# 3. Recrear DB (si es necesario)
docker-compose stop catalog-service
docker volume rm infra_catalog-db-data

# 4. Levantar todo
docker-compose up -d

# 5. Verificar logs
docker-compose logs -f catalog-service
```

**Deberías ver en logs:**
```
Sucursales creadas: DOMINation Buenos Aires Centro y DOMINation Belgrano
```

---

## 🧪 TESTING

### Test 1: Login como providerDemo

```bash
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "providerDemo",
    "password": "provider123"
  }'
```

**Guardar token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"providerDemo","password":"provider123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

### Test 2: Ver Mis Sucursales (200 OK)

```bash
curl -X GET http://localhost:8080/api/catalog/provider/branches \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "name": "DOMINation Buenos Aires Centro",
    "address": "Av. Corrientes 1234, CABA",
    "active": true,
    "providerId": 2
  },
  {
    "id": 2,
    "name": "DOMINation Belgrano",
    "address": "Av. Cabildo 5678, CABA",
    "active": true,
    "providerId": 2
  }
]
```

### Test 3: Crear Sucursal (201 Created)

```bash
curl -X POST http://localhost:8080/api/catalog/provider/branches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "DOMINation Palermo",
    "address": "Av. Santa Fe 3000, CABA"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 3,
  "name": "DOMINation Palermo",
  "address": "Av. Santa Fe 3000, CABA",
  "active": true,
  "providerId": 2
}
```

### Test 4: Editar Sucursal Propia (200 OK)

```bash
curl -X PUT http://localhost:8080/api/catalog/provider/branches/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "DOMINation Centro ACTUALIZADO",
    "address": "Av. Corrientes 1234, CABA"
  }'
```

**Respuesta esperada:** 200 OK con branch actualizada

### Test 5: Eliminar Sucursal Propia (204 No Content)

```bash
curl -X DELETE http://localhost:8080/api/catalog/provider/branches/3 \
  -H "Authorization: Bearer $TOKEN" \
  -v
```

**Respuesta esperada:** `HTTP/1.1 204 No Content`

---

## ❌ TESTS DE SEGURIDAD (403 Forbidden)

### Test 6: Acceder sin Token (401 Unauthorized)

```bash
curl -X GET http://localhost:8080/api/catalog/provider/branches -v
```

**Respuesta esperada:** `HTTP/1.1 401 Unauthorized`

### Test 7: Acceder con Token de Usuario Normal (403 Forbidden)

```bash
# Registrar un usuario normal
curl -X POST http://localhost:9000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "userTest",
    "email": "user@test.com",
    "password": "password123"
  }'

# Login como usuario normal
USER_TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"userTest","password":"password123"}' \
  | jq -r '.token')

# Intentar acceder a endpoint de provider
curl -X GET http://localhost:8080/api/catalog/provider/branches \
  -H "Authorization: Bearer $USER_TOKEN" \
  -v
```

**Respuesta esperada:** `HTTP/1.1 403 Forbidden`

### Test 8: Intentar Editar Sucursal de Otro Provider (403 Forbidden)

```bash
# 1. Crear segundo provider en DB manualmente
docker exec -it domination-postgres-auth psql -U postgres -d auth_db

-- En psql:
BEGIN;

INSERT INTO users (username, email, password, enabled, created_at, updated_at) 
VALUES ('provider2', 'provider2@test.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa', true, NOW(), NOW());

-- Obtener ID del nuevo usuario (ej: 3)
SELECT id FROM users WHERE username = 'provider2';

-- Asignar roles
INSERT INTO user_roles (user_id, role_id) VALUES (3, 2);  -- ROLE_USER
INSERT INTO user_roles (user_id, role_id) VALUES (3, 3);  -- ROLE_PROVIDER

COMMIT;
\q

# 2. Login como provider2
PROVIDER2_TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"provider2","password":"123456admin"}' \
  | jq -r '.token')

# 3. Intentar editar sucursal de providerDemo (id=1)
curl -X PUT http://localhost:8080/api/catalog/provider/branches/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $PROVIDER2_TOKEN" \
  -d '{
    "name": "INTENTO HACKEAR",
    "address": "Hacker St 666"
  }' \
  -v
```

**Respuesta esperada:**
```
HTTP/1.1 403 Forbidden
{
  "error": "Access Denied",
  "message": "No tienes permiso para editar esta sucursal"
}
```

---

## ✅ VERIFICACIÓN DE OWNERSHIP

### Flujo de Validación

1. **Controller** extrae `userId` del JWT:
   ```java
   Long providerId = jwt.getClaim("userId");  // providerId ES userId
   ```

2. **Service** valida ownership:
   ```java
   if (!providerId.equals(branch.getProviderId())) {
       throw new AccessDeniedException("No tienes permiso...");
   }
   ```

3. **Resultado:**
   - ✅ Provider puede ver/editar/eliminar solo SUS sucursales
   - ❌ Provider NO puede ver/editar/eliminar sucursales de otros
   - ✅ Admin puede hacer todo (implementar en siguiente sprint)

---

## 📊 CHECKLIST DE VERIFICACIÓN

- [ ] Columna `provider_id` existe en tabla `branches`
- [ ] `GET /api/catalog/provider/branches` con token PROVIDER retorna 200
- [ ] Branches retornadas tienen `providerId = userId del JWT`
- [ ] `POST /api/catalog/provider/branches` crea branch con providerId correcto
- [ ] `PUT /api/catalog/provider/branches/X` con branch propia retorna 200
- [ ] `PUT /api/catalog/provider/branches/X` con branch ajena retorna 403
- [ ] `DELETE /api/catalog/provider/branches/X` con branch propia retorna 204
- [ ] `DELETE /api/catalog/provider/branches/X` con branch ajena retorna 403
- [ ] Usuario con `ROLE_USER` (sin PROVIDER) recibe 403
- [ ] Request sin token recibe 401

---

## 🎯 PRÓXIMOS PASOS

1. ✅ **COMPLETADO**: Provider ownership en catalog-service
2. ⏭️ **SIGUIENTE**: Endpoints de items (salas) para provider
3. ⏭️ **SIGUIENTE**: AdminController para acceso total
4. ⏭️ **SIGUIENTE**: Provider ownership en booking-service

Ver `ARQUITECTURA_ROLES_Y_PERMISOS.md` para roadmap completo.

---

**Fecha:** 2026-01-08  
**Estado:** ✅ IMPLEMENTADO Y LISTO PARA TESTING  
**Versión:** Sprint 1 - Ownership basado en userId

