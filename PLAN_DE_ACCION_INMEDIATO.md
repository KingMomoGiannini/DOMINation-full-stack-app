# ⚡ PLAN DE ACCIÓN INMEDIATO - Implementación de Roles

## 🎯 RESUMEN EJECUTIVO

**Objetivo:** Replicar la lógica de roles del monolito DOMINation en la arquitectura de microservicios V2.

**Roles Identificados:**
- **Administrador** (monolito) → `ROLE_ADMIN` (V2) ✅ Ya existe
- **Cliente** (monolito) → `ROLE_USER` (V2) ✅ Ya existe  
- **Prestador** (monolito) → `ROLE_PROVIDER` (V2) ⚠️ **CREAR**

**Documento Completo:** Ver `ARQUITECTURA_ROLES_Y_PERMISOS.md` para detalles exhaustivos.

---

## 🚀 SPRINT 1 - ACCIONES PRIORITARIAS (Esta Semana)

### 📌 PASO 1: AUTH-SERVICE - Agregar Rol PROVIDER (30 min)

#### 1.1 Actualizar RoleName Enum
**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/entity/RoleName.java`

```java
public enum RoleName {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_PROVIDER  // ← AGREGAR ESTA LÍNEA
}
```

#### 1.2 Actualizar DataSeeder
**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/config/DataSeeder.java`

En el método `seedRoles()`, después de crear ROLE_USER, agregar:

```java
// Crear ROLE_PROVIDER
if (roleRepository.findByName(RoleName.ROLE_PROVIDER).isEmpty()) {
    Role providerRole = Role.builder()
            .name(RoleName.ROLE_PROVIDER)
            .build();
    roleRepository.save(providerRole);
    log.info("✅ Rol ROLE_PROVIDER creado");
} else {
    log.info("ℹ️ Rol ROLE_PROVIDER ya existe");
}
```

#### 1.3 Rebuild y Restart Auth-Service

```bash
cd services/auth-service
mvn clean package -DskipTests
cd ../../infra
docker-compose build auth-service
docker-compose up -d auth-service
```

**Verificar logs:**
```bash
docker-compose logs auth-service | grep PROVIDER
```

Deberías ver: `✅ Rol ROLE_PROVIDER creado`

---

### 📌 PASO 2: CATALOG-SERVICE - Agregar providerId a Branch (45 min)

#### 2.1 Actualizar Entity Branch
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/domain/Branch.java`

```java
@Column(name = "provider_id")
private Long providerId;

// En el constructor que acepta todos los campos, agregar:
public Branch(Long id, String name, String address, boolean active, Long providerId) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.active = active;
    this.providerId = providerId;  // ← AGREGAR
}

// Getter y Setter
public Long getProviderId() {
    return providerId;
}

public void setProviderId(Long providerId) {
    this.providerId = providerId;
}
```

#### 2.2 Actualizar BranchDTO
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/dto/BranchDTO.java`

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {
    private Long id;
    private String name;
    private String address;
    private boolean active;
    private Long providerId;  // ← AGREGAR
}
```

#### 2.3 Actualizar Mapper
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/mapper/BranchMapper.java`

```java
public BranchDTO toDTO(Branch branch) {
    if (branch == null) return null;
    return new BranchDTO(
        branch.getId(),
        branch.getName(),
        branch.getAddress(),
        branch.isActive(),
        branch.getProviderId()  // ← AGREGAR
    );
}
```

#### 2.4 Actualizar DataSeeder
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/config/DataSeeder.java`

En las líneas donde creas branches, agregar providerId:

```java
Branch branch1 = new Branch();
branch1.setName("Sede Centro");
branch1.setAddress("Av. Corrientes 1234, CABA");
branch1.setActive(true);
branch1.setProviderId(1L);  // ← AGREGAR (ID del provider de ejemplo)
```

#### 2.5 Ejecutar SQL Migration

**Opción A - Manual (Recomendado para desarrollo):**

```sql
-- Conectarse a la DB catalog
docker exec -it domination-postgres-catalog psql -U domination -d domination_catalog

-- Agregar columna
ALTER TABLE branch ADD COLUMN provider_id BIGINT;

-- Actualizar datos existentes (asignar a un provider de ejemplo)
UPDATE branch SET provider_id = 1 WHERE provider_id IS NULL;

-- Salir
\q
```

**Opción B - Borrar y Recrear (CUIDADO: borra datos):**

```bash
cd infra
docker-compose down
docker volume rm infra_catalog-db-data
docker-compose up -d
```

---

### 📌 PASO 3: CATALOG-SERVICE - Habilitar Method Security (15 min)

#### 3.1 Actualizar SecurityConfig
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← AGREGAR ESTA ANOTACIÓN
public class SecurityConfig {
    // ... resto del código igual
}
```

---

### 📌 PASO 4: CATALOG-SERVICE - Crear ProviderController (1 hora)

#### 4.1 Crear Nuevo Controller
**Archivo (NUEVO):** `services/catalog-service/src/main/java/com/domination/catalog/controller/ProviderController.java`

```java
package com.domination.catalog.controller;

import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/provider")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderController {
    
    private final BranchService branchService;
    
    @GetMapping("/branches")
    public ResponseEntity<List<BranchDTO>> getMyBranches(
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        List<BranchDTO> branches = branchService.findByProviderId(providerId);
        return ResponseEntity.ok(branches);
    }
    
    @PostMapping("/branches")
    public ResponseEntity<BranchDTO> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        BranchDTO created = branchService.createForProvider(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/branches/{id}")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        BranchDTO updated = branchService.updateForProvider(id, request, providerId);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        branchService.deleteForProvider(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    private Long extractProviderId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new IllegalStateException("userId no encontrado en JWT");
        }
        // El providerId ES el userId del usuario con rol PROVIDER
        // Convertir a Long (puede venir como Integer o Long dependiendo del JSON)
        if (userIdClaim instanceof Integer) {
            return ((Integer) userIdClaim).longValue();
        }
        return (Long) userIdClaim;
    }
}
```

#### 4.2 Actualizar BranchService
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/service/BranchService.java`

Agregar métodos:

```java
import org.springframework.security.access.AccessDeniedException;
import java.util.stream.Collectors;

// ... dentro de la clase BranchService:

public List<BranchDTO> findByProviderId(Long providerId) {
    List<Branch> branches = branchRepository.findByProviderId(providerId);
    return branches.stream()
            .map(branchMapper::toDTO)
            .collect(Collectors.toList());
}

public BranchDTO createForProvider(CreateBranchRequest request, Long providerId) {
    Branch branch = new Branch();
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    branch.setActive(true);
    branch.setProviderId(providerId);  // Auto-asignar desde JWT
    
    Branch saved = branchRepository.save(branch);
    return branchMapper.toDTO(saved);
}

public BranchDTO updateForProvider(Long id, CreateBranchRequest request, Long providerId) {
    Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    
    // VALIDAR OWNERSHIP
    if (!providerId.equals(branch.getProviderId())) {
        throw new AccessDeniedException("No tienes permiso para editar esta sucursal");
    }
    
    branch.setName(request.getName());
    branch.setAddress(request.getAddress());
    
    Branch updated = branchRepository.save(branch);
    return branchMapper.toDTO(updated);
}

public void deleteForProvider(Long id, Long providerId) {
    Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
    
    // VALIDAR OWNERSHIP
    if (!providerId.equals(branch.getProviderId())) {
        throw new AccessDeniedException("No tienes permiso para eliminar esta sucursal");
    }
    
    branchRepository.deleteById(id);
}
```

#### 4.3 Actualizar BranchRepository
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/repository/BranchRepository.java`

```java
import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActive(boolean active);
    List<Branch> findByProviderId(Long providerId);  // ← AGREGAR
}
```

#### 4.4 Crear Excepción AccessDeniedException
**Archivo (NUEVO):** `services/catalog-service/src/main/java/com/domination/catalog/exception/AccessDeniedException.java`

```java
package com.domination.catalog.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
```

#### 4.5 Actualizar GlobalExceptionHandler
**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/exception/GlobalExceptionHandler.java`

Agregar handler:

```java
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Access Denied");
    error.put("detail", ex.getMessage());
    error.put("timestamp", LocalDateTime.now().toString());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

---

### 📌 PASO 5: Rebuild y Test (30 min)

#### 5.1 Compilar Servicios

```bash
cd services/catalog-service
mvn clean package -DskipTests

cd ../booking-service
mvn clean package -DskipTests

cd ../../infra
docker-compose build catalog-service booking-service
docker-compose up -d
```

#### 5.2 Verificar Logs

```bash
docker-compose logs -f catalog-service
docker-compose logs -f auth-service
```

#### 5.3 Testing con curl

```bash
# 1. Registrar un provider (manualmente en DB por ahora)
docker exec -it domination-postgres-auth psql -U postgres -d auth_db

# En psql:
BEGIN;

-- Obtener el rol PROVIDER
SELECT * FROM roles WHERE name = 'ROLE_PROVIDER';
-- Supongamos que id=3

-- Crear un usuario de prueba
INSERT INTO users (username, email, password, enabled, created_at, updated_at) 
VALUES ('providerTest', 'provider@test.com', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRps.9cGLcZEiGDMVr5yUP1KUOYTa', true, NOW(), NOW());

-- Obtener el ID del usuario
SELECT id FROM users WHERE username = 'providerTest';
-- Supongamos que id=2

-- Asignar roles (USER y PROVIDER)
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);  -- ROLE_USER
INSERT INTO user_roles (user_id, role_id) VALUES (2, 3);  -- ROLE_PROVIDER

COMMIT;
\q

# 2. Login como provider
curl -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "providerTest",
    "password": "123456admin"
  }'

# Copiar el token de la respuesta

# 3. Crear una sucursal como provider
curl -X POST http://localhost:8080/api/catalog/provider/branches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -d '{
    "name": "Mi Sede de Prueba",
    "address": "Calle Falsa 123"
  }'

# 4. Ver mis sucursales
curl -X GET http://localhost:8080/api/catalog/provider/branches \
  -H "Authorization: Bearer TU_TOKEN_AQUI"
```

---

## ✅ CHECKLIST DE VERIFICACIÓN

Antes de considerar el Sprint 1 completo:

- [ ] `ROLE_PROVIDER` existe en la tabla `roles` de auth_db
- [ ] Se puede crear un usuario con rol PROVIDER
- [ ] El JWT generado incluye `ROLE_PROVIDER` en authorities
- [ ] La tabla `branch` tiene columna `provider_id`
- [ ] Se puede crear una sucursal con `POST /api/catalog/provider/branches`
- [ ] La sucursal creada tiene el `providerId` correcto
- [ ] Un provider no puede editar sucursales de otro provider (403)
- [ ] Un usuario con `ROLE_USER` no puede acceder a endpoints de provider (403)
- [ ] El admin (`adminSeba`) puede seguir accediendo a `/api/catalog/admin/**`
- [ ] Los endpoints públicos siguen funcionando sin token

---

## 🐛 TROUBLESHOOTING

### Error: "providerId no encontrado en JWT"

**Causa:** El campo `providerId` no se está incluyendo en el JWT al hacer login.

**Solución:** Debes agregar el código en `OAuth2TokenService` para incluir el claim. Ver sección C.1.4 del documento completo.

### Error: "Access Denied" al crear sucursal

**Causa:** El usuario no tiene `ROLE_PROVIDER` o el JWT no tiene el claim `authorities` correcto.

**Solución:**
1. Verificar roles del usuario en DB
2. Verificar el JWT decodificado en https://jwt.io
3. Asegurar que el claim `authorities` es un array: `["ROLE_PROVIDER", "ROLE_USER"]`

### Error 401 en todos los endpoints protegidos

**Causa:** El Resource Server no puede validar el JWT (problema con issuer-uri).

**Solución:**
1. Verificar que auth-service esté corriendo: `docker-compose ps`
2. Verificar logs de catalog/booking: `docker-compose logs catalog-service | grep JWT`
3. Verificar que el issuer-uri sea accesible desde el contenedor

### Column "provider_id" does not exist

**Causa:** No ejecutaste la migración SQL.

**Solución:** Ver Paso 2.5

---

## 📞 PRÓXIMOS PASOS (Sprint 2)

Una vez completado Sprint 1, continuar con:

1. **AUTH-SERVICE**: Implementar campo `providerId` en User entity
2. **AUTH-SERVICE**: Incluir `providerId` en JWT claims
3. **CATALOG-SERVICE**: Endpoints para CRUD de items (salas/instrumentos)
4. **BOOKING-SERVICE**: Endpoint GET `/api/booking/reservations/{id}` con validación de ownership
5. **BOOKING-SERVICE**: ProviderReservationController
6. **BOOKING-SERVICE**: AdminReservationController

Ver documento completo `ARQUITECTURA_ROLES_Y_PERMISOS.md` sección D.1 para detalles.

---

**Última Actualización:** 2026-01-08  
**Estimación de Tiempo Total Sprint 1:** 3-4 horas  
**Estado:** ✅ LISTO PARA IMPLEMENTAR

