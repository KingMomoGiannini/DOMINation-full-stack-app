# 🏗️ ARQUITECTURA DE ROLES Y PERMISOS - DOMINation V2

## 📋 ÍNDICE
1. [Auditoría Proyecto Monolito](#auditoría-proyecto-monolito)
2. [Diseño V2 - Microservicios](#diseño-v2---microservicios)
3. [Implementación Detallada](#implementación-detallada)
4. [TODOs y Roadmap](#todos-y-roadmap)

---

## 🔍 AUDITORÍA PROYECTO MONOLITO

### A.1) ROLES DETECTADOS

El proyecto monolito usa **herencia de tabla única (JOINED)** con tres tipos de usuarios:

| Rol | Clase Java | Campo Discriminador | Tabla DB | Evidence |
|-----|-----------|-------------------|----------|-----------|
| **Administrador** | `Administrador.java` | `rol = "administrador"` | `administrador` (separada) | Línea 27 SecurityConfig |
| **Prestador** | `Prestador extends Usuario` | `rol = "prestador"` | `usuario` + `prestador` (JOINED) | Línea 11 models/Prestador.java |
| **Cliente** | `Cliente extends Usuario` | `rol = "cliente"` | `usuario` + `cliente` (JOINED) | Línea 15 models/Cliente.java |

**Evidencia Código:**
- `Usuario.java:41` - Campo `rol` tipo String en la entidad base
- `SecurityConfig.java:36-40` - Definición de permisos por rol
- `CustomUserDetailsService.java:44-52` - Asignación de authorities con prefijo `ROLE_`

### A.2) ARQUITECTURA DE AUTENTICACIÓN (Monolito)

```
┌─────────────────────┐
│   Login Form        │ (JSP)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ CustomUserDetails   │ - Busca en UsuarioRepository
│ Service             │ - Busca en AdministradorRepository
└──────────┬──────────┘ - Asigna ROLE_{rol}
           │
           ▼
┌─────────────────────┐
│ Spring Security     │ - hasRole("cliente")
│ Filter Chain        │ - hasRole("prestador")
└──────────┬──────────┘ - hasRole("administrador")
           │
           ▼
┌─────────────────────┐
│ HttpSession         │ - userLogueado (Usuario object)
│                     │ - Usado en controllers para
└─────────────────────┘   validaciones adicionales
```

**Mecanismos de Control:**
1. **SecurityFilterChain** (`SecurityConfig.java:31-59`): autorización por URL pattern
2. **Session Checks** (Controllers): validaciones adicionales de "ownership" (ej: `validarSucuPrest()`)
3. **Instance checking** (`instanceof`): discriminar entre Cliente/Prestador/Admin en runtime

### A.3) TABLA DE PERMISOS - PROYECTO MONOLITO

#### 📍 ENDPOINTS PÚBLICOS (sin autenticación)
| Endpoint | Descripción |
|----------|-------------|
| `/` | Landing page |
| `/login` | Formulario de login |
| `/registrarse` | Formulario de registro |
| `/css/**`, `/js/**`, `/img/**` | Recursos estáticos |

#### 🔒 ENDPOINTS PROTEGIDOS

| Acción | URL Pattern | Cliente | Prestador | Admin | Evidencia | Notas Adicionales |
|--------|------------|---------|-----------|-------|-----------|-------------------|
| **Ver inicio/home** | `/inicio` | ✅ | ✅ | ✅ | SecurityConfig.java:36 | Todos los roles |
| **Listar sucursales disponibles** | `/salas/salasDisponibles/**` | ✅ | ✅ | ✅ | SecurityConfig.java:37 | Consulta pública |
| **Crear sucursal** | `/sedes/create` | ❌ | ✅ | ✅ | SecurityConfig.java:38 | Solo puede crear sus propias sedes |
| **Editar sucursal** | `/sedes/update` | ❌ | ✅ (propias) | ✅ (todas) | SucursalController.java:51-71 | Validación `validarSucuPrest()` verifica ownership |
| **Eliminar sucursal** | `/sedes/delete` | ❌ | ✅ (propias) | ✅ (todas) | SucursalController.java:74-95 | Validación de ownership línea 84 |
| **Crear sala** | `/salas/create` | ❌ | ✅ (en sus sedes) | ✅ | SecurityConfig.java:38 | Debe ser de una sucursal propia |
| **Editar sala** | `/salas/update` | ❌ | ✅ (propias) | ✅ | SecurityConfig.java:38 | Ownership via Sucursal |
| **Eliminar sala** | `/salas/delete` | ❌ | ✅ (propias) | ✅ | SecurityConfig.java:38 | Ownership via Sucursal |
| **Crear reserva** | `/reservas/create` | ✅ | ✅ | ✅ | ReservaController.java:43-59 | Cliente para sí mismo |
| **Editar reserva** | `/reservas/edit` | ✅ (propias) | ✅ | ✅ | SecurityConfig.java:37 | Solo sus propias reservas |
| **Eliminar reserva** | `/reservas/delete` | ✅ (propias) | ✅ | ✅ | ReservaController.java:188-200 | Solo sus propias reservas |
| **Ver mis reservas** | `/reservas/misReservas` | ✅ | ❌ | ❌ | ReservaController.java:100-118 | Endpoint exclusivo Cliente |
| **Ver reservas de mis sedes** | `/reservas/listaReservas` | ❌ | ✅ | ❌ | ReservaController.java:62-77 | Validación idPrestador línea 65 |
| **Ver todas las reservas (admin)** | `/reservas/admin/listaReservas` | ❌ | ❌ | ✅ | ReservaController.java:80-96 | Endpoint exclusivo Admin |
| **Ver mi cuenta** | `/usuarios/Micuenta/**` | ✅ | ❌ | ❌ | SecurityConfig.java:39 | Editar datos propios |
| **CRUD usuarios** | `/usuarios/**` | ❌ | ❌ | ✅ | SecurityConfig.java:40 | Alta/baja/modificación |

#### 🔑 VALIDACIONES ADICIONALES EN CÓDIGO (Ownership)

| Validación | Ubicación | Lógica |
|------------|-----------|--------|
| **Sucursal pertenece a Prestador** | `SucursalController.java:221-225` | `validarSucuPrest()`: compara `sucursal.getPrestador().getIdPrestador()` con prestador en sesión |
| **Cliente solo ve sus reservas** | `ReservaController.java:103-104` | Compara `idCliente` de parámetro con `usuarioSesion.getIdCliente()` |
| **Prestador solo ve reservas de sus sedes** | `ReservaController.java:65-66` | Compara `idPrestador` con prestador en sesión |
| **Admin puede ver cualquier reserva** | `ReservaController.java:82-85` | No valida ownership, solo identidad |

### A.4) MODELO DE DATOS - RELACIONES CLAVE

```
Administrador (tabla separada)
      ↓ 1:N (via FK administrador_idadministrador)
Usuario (abstract)
      ├─→ Cliente
      │     └─→ 1:N Reserva
      └─→ Prestador
            └─→ 1:N Sucursal
                  ├─→ 1:1 Domicilio
                  └─→ 1:N Sala
                        └─→ 1:N Reserva
```

**Propiedad/Ownership**:
- `Sucursal.prestador_idprestador` (FK) → determina quién es dueño
- `Reserva.cliente_idusuario` (FK) → determina quién hizo la reserva
- `Sala.sucursal_idsucursal` (FK) → determina pertenencia

---

## 🎯 DISEÑO V2 - MICROSERVICIOS

### B.1) MAPEO DE ROLES V1 → V2

| Rol Monolito | Rol V2 (JWT Authority) | RoleName Enum | Decisión |
|--------------|------------------------|---------------|----------|
| `administrador` | `ROLE_ADMIN` | `ROLE_ADMIN` | ✅ Ya existe en auth-service |
| `cliente` | `ROLE_USER` | `ROLE_USER` | ✅ Ya existe en auth-service |
| `prestador` | `ROLE_PROVIDER` | `ROLE_PROVIDER` | ⚠️ **NUEVO** - Crear en auth-service |

**Justificación:**
- `ROLE_USER` es más estándar que "cliente" en arquitecturas modernas
- `ROLE_PROVIDER` es más descriptivo y extensible (futuro: proveedores de instrumentos, accesorios, etc.)
- Mantiene compatibilidad con Spring Security (`ROLE_` prefix)

### B.2) CLAIMS JWT REQUERIDOS

```json
{
  "sub": "adminSeba",
  "iss": "http://localhost:9000",
  "exp": 1735497600,
  "iat": 1735494000,
  "authorities": ["ROLE_ADMIN", "ROLE_USER"],
  "userId": "1",
  "email": "admin@domination.com",
  "customClaims": {
    "providerId": 123,       // Solo para ROLE_PROVIDER
    "branchIds": [1, 2, 3]   // Solo para ROLE_PROVIDER (futuro)
  }
}
```

**Claims Críticos:**
- `authorities`: Lista de roles (usado por Spring Security)
- `userId`: ID del usuario en auth DB (usado para auditoría, logs Y como providerId para ownership)

### B.3) ARQUITECTURA DE PERMISOS V2

```
┌─────────────────────┐
│  Frontend React     │ Login → POST /auth/login
└──────────┬──────────┘
           │ JWT en header
           ▼
┌─────────────────────┐
│  API Gateway        │ - CORS
│  :8080              │ - NO valida JWT (pass-through)
└──────────┬──────────┘ - Routing
           │
     ┌─────┴─────────────────┐
     │                       │
     ▼                       ▼
┌──────────────┐      ┌──────────────┐
│ catalog      │      │ booking      │
│ service      │      │ service      │
│ :8081        │      │ :8082        │
└──────────────┘      └──────────────┘
     │                       │
     ├─ Resource Server      ├─ Resource Server
     ├─ Valida JWT           ├─ Valida JWT
     ├─ Extrae authorities   ├─ Extrae authorities
     └─ @PreAuthorize        └─ @PreAuthorize
           │                       │
           └───────┬───────────────┘
                   ▼
          ┌──────────────┐
          │ auth-service │ issuer-uri
          │ :9000        │ (JWK endpoint)
          └──────────────┘
```

### B.4) TABLA DE PERMISOS V2 - POR MICROSERVICIO

#### 🌐 CATALOG-SERVICE (Puerto 8081)

| Endpoint | Método | Rol Requerido | Validación Adicional | Nueva Config |
|----------|--------|---------------|---------------------|--------------|
| `/api/catalog/branches` | GET | **PUBLIC** | Ninguna | ✅ Ya existe |
| `/api/catalog/branches/{id}` | GET | **PUBLIC** | Ninguna | ✅ Ya existe |
| `/api/catalog/items` | GET | **PUBLIC** | Ninguna (filtrar por branchId) | ✅ Ya existe |
| `/api/catalog/items/{id}` | GET | **PUBLIC** | Ninguna | ✅ Ya existe |
| `/api/catalog/items/{id}/inventory` | GET | **PUBLIC** | Ninguna | ⚠️ **CREAR** endpoint |
| `/api/catalog/branches` | POST | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: validar que no exista `providerId` en body (auto-asignar desde JWT). Admin: puede asignar cualquier providerId | ⚠️ **MODIFICAR** - agregar validación |
| `/api/catalog/branches/{id}` | PUT | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: `branch.providerId == JWT.providerId`. Admin: sin restricción | ⚠️ **MODIFICAR** - agregar validación |
| `/api/catalog/branches/{id}` | DELETE | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: `branch.providerId == JWT.providerId`. Admin: sin restricción | ⚠️ **CREAR** endpoint + validación |
| `/api/catalog/branches/{branchId}/items` | POST | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: la branch debe ser propia. Admin: sin restricción | ⚠️ **CREAR** endpoint + validación |
| `/api/catalog/items/{id}` | PUT | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: `item.branch.providerId == JWT.providerId`. Admin: sin restricción | ⚠️ **CREAR** endpoint + validación |
| `/api/catalog/items/{id}` | DELETE | `ROLE_PROVIDER` o `ROLE_ADMIN` | Provider: `item.branch.providerId == JWT.providerId`. Admin: sin restricción | ⚠️ **CREAR** endpoint + validación |
| `/api/catalog/admin/**` | ALL | `ROLE_ADMIN` | Sin validación adicional | ✅ Ya existe |

#### 📅 BOOKING-SERVICE (Puerto 8082)

| Endpoint | Método | Rol Requerido | Validación Adicional | Nueva Config |
|----------|--------|---------------|---------------------|--------------|
| `/api/booking/reservations` | POST | `ROLE_USER` | `customerId` debe coincidir con JWT.userId | ✅ Ya existe (ajustar validación) |
| `/api/booking/my/reservations` | GET | `ROLE_USER` | Filtrar por `customerId == JWT.userId` | ✅ Ya existe |
| `/api/booking/reservations/{id}` | GET | `ROLE_USER`, `ROLE_PROVIDER`, `ROLE_ADMIN` | User: solo si es suya. Provider: solo si es de su branch. Admin: cualquiera | ⚠️ **CREAR** endpoint + validación |
| `/api/booking/reservations/{id}` | PUT | `ROLE_USER` o `ROLE_ADMIN` | User: solo si es suya. Admin: cualquiera | ⚠️ **CREAR** endpoint + validación |
| `/api/booking/reservations/{id}` | DELETE | `ROLE_USER` o `ROLE_ADMIN` | User: solo si es suya. Admin: cualquiera | ⚠️ **CREAR** endpoint + validación |
| `/api/booking/provider/reservations` | GET | `ROLE_PROVIDER` | Filtrar reservas donde `item.branch.providerId == JWT.providerId` | ⚠️ **CREAR** endpoint |
| `/api/booking/provider/reservations/{id}/payment` | POST | `ROLE_PROVIDER` | Registrar pago. Validar que la reserva pertenezca a una de sus branches | ⚠️ **FUTURO** (payments-service) |
| `/api/booking/admin/reservations` | GET | `ROLE_ADMIN` | Sin filtro | ⚠️ **CREAR** endpoint |
| `/api/booking/admin/reservations/{id}/status` | PATCH | `ROLE_ADMIN` | Cambiar estado (PENDING/CONFIRMED/CANCELLED) | ⚠️ **CREAR** endpoint |

#### 🔐 AUTH-SERVICE (Puerto 9000)

| Endpoint | Método | Rol Requerido | Notas |
|----------|--------|---------------|-------|
| `/auth/register` | POST | **PUBLIC** | Asigna `ROLE_USER` por defecto |
| `/auth/login` | POST | **PUBLIC** | Genera JWT con authorities |
| `/auth/register/provider` | POST | **PUBLIC** o `ROLE_ADMIN` | ⚠️ **CREAR** - Asigna `ROLE_USER` + `ROLE_PROVIDER` |
| `/users/me` | GET | **AUTHENTICATED** | Devuelve datos del usuario actual |
| `/admin/users` | GET | `ROLE_ADMIN` | Listar todos los usuarios |
| `/admin/users/{id}` | PUT | `ROLE_ADMIN` | Editar usuario (cambiar roles, habilitar/deshabilitar) |

---

## 🛠️ IMPLEMENTACIÓN DETALLADA

### C.1) CAMBIOS EN AUTH-SERVICE

#### 1️⃣ Agregar `ROLE_PROVIDER` al enum

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/entity/RoleName.java`

```java
package com.gianniniseba.authservice.entity;

public enum RoleName {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_PROVIDER  // ← AGREGAR
}
```

#### 2️⃣ Actualizar DataSeeder para crear rol PROVIDER

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/config/DataSeeder.java`

```java
// En el método seedRoles(), agregar:

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

#### 3️⃣ Agregar campo `providerId` a User entity (opcional, para futuro)

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/entity/User.java`

```java
@Column(name = "provider_id")
private Long providerId;  // NULL si no es provider

// Getter/Setter
public Long getProviderId() {
    return providerId;
}

public void setProviderId(Long providerId) {
    this.providerId = providerId;
}
```

#### 4️⃣ Incluir `providerId` en JWT claims

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/service/OAuth2TokenService.java`

Buscar el método `generateAccessToken` y agregar claim custom:

```java
// Agregar claim providerId si el usuario tiene ROLE_PROVIDER
if (user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ROLE_PROVIDER)) {
    if (user.getProviderId() != null) {
        claimsBuilder.claim("providerId", user.getProviderId());
    }
}
```

#### 5️⃣ Endpoint para registro de providers

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/controller/AuthController.java`

```java
@PostMapping("/register/provider")
public ResponseEntity<AuthResponse> registerProvider(@Valid @RequestBody RegisterRequest request){
    AuthResponse response = authService.registerProvider(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**Archivo:** `services/auth-service/src/main/java/com/gianniniseba/authservice/service/AuthService.java`

```java
public AuthResponse registerProvider(RegisterRequest request) {
    if(userRepository.existsByUsername(request.getUsername())){
        throw new UserAlreadyExistsException("El nombre de usuario ingresado ya se encuentra en uso.");
    }
    
    if(userRepository.existsByEmail(request.getEmail())){
        throw new UserAlreadyExistsException("El email ingresado ya se encuentra en uso.");
    }
    
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    
    Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
            .orElseThrow(() -> new IllegalStateException("ROLE_USER no configurado"));
    Role providerRole = roleRepository.findByName(RoleName.ROLE_PROVIDER)
            .orElseThrow(() -> new IllegalStateException("ROLE_PROVIDER no configurado"));
    
    User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(encodedPassword)
            .enabled(true)
            .roles(Set.of(userRole, providerRole))  // Ambos roles
            .build();
    
    userRepository.save(user);
    
    return AuthResponse.builder()
            .message("Provider registrado exitosamente.")
            .token(null)
            .build();
}
```

### C.2) CAMBIOS EN CATALOG-SERVICE

#### 1️⃣ Agregar campo `providerId` a Branch entity

**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/domain/Branch.java`

```java
@Column(name = "provider_id")
private Long providerId;  // ID del prestador dueño

// Constructor, getters, setters
```

#### 2️⃣ Actualizar SecurityConfig para permisos granulares

**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Habilitar @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers(HttpMethod.GET, "/api/catalog/branches/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/items/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Endpoints protegidos
                .requestMatchers("/api/catalog/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        grantedAuthoritiesConverter.setAuthorityPrefix("");  // Ya viene con ROLE_
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
```

#### 3️⃣ Crear AdminController con endpoints CRUD completos

**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/controller/AdminController.java`

Agregar endpoints faltantes (ya existe parcialmente):

```java
// POST /api/catalog/admin/branches/{branchId}/items
@PostMapping("/branches/{branchId}/items")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ItemDTO> createItemForBranch(
        @PathVariable Long branchId,
        @Valid @RequestBody CreateItemRequest request) {
    // Implementar
}

// PUT /api/catalog/admin/items/{id}
@PutMapping("/items/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ItemDTO> updateItem(
        @PathVariable Long id,
        @Valid @RequestBody CreateItemRequest request) {
    // Implementar
}

// DELETE /api/catalog/admin/items/{id}
@DeleteMapping("/items/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
    // Implementar
}
```

#### 4️⃣ Crear ProviderController para gestión de propias sucursales

**Archivo (NUEVO):** `services/catalog-service/src/main/java/com/domination/catalog/controller/ProviderController.java`

```java
package com.domination.catalog.controller;

import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.dto.CreateItemRequest;
import com.domination.catalog.service.BranchService;
import com.domination.catalog.service.ItemService;
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
    private final ItemService itemService;
    
    // GET /api/catalog/provider/branches - Ver solo mis sucursales
    @GetMapping("/branches")
    public ResponseEntity<List<BranchDTO>> getMyBranches(
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        if (providerId == null) {
            throw new IllegalStateException("providerId no encontrado en JWT");
        }
        List<BranchDTO> branches = branchService.findByProviderId(providerId);
        return ResponseEntity.ok(branches);
    }
    
    // POST /api/catalog/provider/branches - Crear sucursal propia
    @PostMapping("/branches")
    public ResponseEntity<BranchDTO> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        if (providerId == null) {
            throw new IllegalStateException("providerId no encontrado en JWT");
        }
        // Forzar que la branch sea del provider autenticado
        BranchDTO created = branchService.createForProvider(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // PUT /api/catalog/provider/branches/{id} - Editar sucursal propia
    @PutMapping("/branches/{id}")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        // Validar ownership
        BranchDTO updated = branchService.updateForProvider(id, request, providerId);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /api/catalog/provider/branches/{id} - Eliminar sucursal propia
    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        branchService.deleteForProvider(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    // POST /api/catalog/provider/branches/{branchId}/items
    @PostMapping("/branches/{branchId}/items")
    public ResponseEntity<ItemDTO> createItem(
            @PathVariable Long branchId,
            @Valid @RequestBody CreateItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        // Validar que branchId pertenece al provider
        ItemDTO created = itemService.createForProvider(branchId, request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // PUT /api/catalog/provider/items/{id}
    @PutMapping("/items/{id}")
    public ResponseEntity<ItemDTO> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateItemRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        ItemDTO updated = itemService.updateForProvider(id, request, providerId);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /api/catalog/provider/items/{id}
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        itemService.deleteForProvider(id, providerId);
        return ResponseEntity.noContent().build();
    }
}
```

#### 5️⃣ Agregar métodos de servicio con validación de ownership

**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/service/BranchService.java`

```java
// Agregar métodos:

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
    branch.setProviderId(providerId);  // Auto-asignar
    
    Branch saved = branchRepository.save(branch);
    return branchMapper.toDTO(saved);
}

public BranchDTO updateForProvider(Long id, CreateBranchRequest request, Long providerId) {
    Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    
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
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    
    // VALIDAR OWNERSHIP
    if (!providerId.equals(branch.getProviderId())) {
        throw new AccessDeniedException("No tienes permiso para eliminar esta sucursal");
    }
    
    branchRepository.deleteById(id);
}
```

#### 6️⃣ Agregar método findByProviderId al repository

**Archivo:** `services/catalog-service/src/main/java/com/domination/catalog/repository/BranchRepository.java`

```java
import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByActive(boolean active);
    List<Branch> findByProviderId(Long providerId);  // ← AGREGAR
}
```

#### 7️⃣ Crear excepción AccessDeniedException

**Archivo (NUEVO):** `services/catalog-service/src/main/java/com/domination/catalog/exception/AccessDeniedException.java`

```java
package com.domination.catalog.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
```

**Actualizar GlobalExceptionHandler:**

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
    Map<String, String> error = new HashMap<>();
    error.put("error", "Access Denied");
    error.put("detail", ex.getMessage());
    error.put("timestamp", LocalDateTime.now().toString());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
}
```

### C.3) CAMBIOS EN BOOKING-SERVICE

#### 1️⃣ Agregar endpoint GET /api/booking/reservations/{id}

**Archivo:** `services/booking-service/src/main/java/com/domination/booking/controller/ReservationController.java`

```java
// GET /api/booking/reservations/{id}
@GetMapping("/reservations/{id}")
public ResponseEntity<ReservationDTO> getReservationById(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt) {
    
    String userId = jwt.getSubject();
    List<String> authorities = jwt.getClaim("authorities");
    
    Reservation reservation = reservationService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    
    // Validar acceso
    if (authorities.contains("ROLE_ADMIN")) {
        // Admin puede ver cualquiera
    } else if (authorities.contains("ROLE_PROVIDER")) {
        Long providerId = jwt.getClaim("providerId");
        // Validar que la reserva es de una de sus branches
        if (!reservationService.belongsToProvider(reservation, providerId)) {
            throw new AccessDeniedException("No tienes permiso para ver esta reserva");
        }
    } else if (authorities.contains("ROLE_USER")) {
        // User solo puede ver sus propias reservas
        if (!userId.equals(reservation.getCustomerId())) {
            throw new AccessDeniedException("No tienes permiso para ver esta reserva");
        }
    }
    
    ReservationDTO dto = reservationMapper.toDTO(reservation);
    return ResponseEntity.ok(dto);
}
```

#### 2️⃣ Crear ProviderController para reservas de sus sucursales

**Archivo (NUEVO):** `services/booking-service/src/main/java/com/domination/booking/controller/ProviderReservationController.java`

```java
package com.domination.booking.controller;

import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking/provider")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderReservationController {
    
    private final ReservationService reservationService;
    
    // GET /api/booking/provider/reservations
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationDTO>> getProviderReservations(
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = jwt.getClaim("providerId");
        if (providerId == null) {
            throw new IllegalStateException("providerId no encontrado en JWT");
        }
        
        List<ReservationDTO> reservations = reservationService.findByProviderId(providerId);
        return ResponseEntity.ok(reservations);
    }
}
```

#### 3️⃣ Agregar método en ReservationService

**Archivo:** `services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`

```java
public List<ReservationDTO> findByProviderId(Long providerId) {
    // 1. Obtener todas las branches del provider desde catalog-service
    List<Long> branchIds = catalogClient.getBranchIdsByProviderId(providerId);
    
    // 2. Para cada branch, obtener sus items
    List<Long> itemIds = new ArrayList<>();
    for (Long branchId : branchIds) {
        List<Long> branchItemIds = catalogClient.getItemIdsByBranchId(branchId);
        itemIds.addAll(branchItemIds);
    }
    
    // 3. Buscar reservas que tengan esos items
    List<Reservation> reservations = reservationRepository.findByItemIdIn(itemIds);
    
    return reservations.stream()
            .map(reservationMapper::toDTO)
            .collect(Collectors.toList());
}

public boolean belongsToProvider(Reservation reservation, Long providerId) {
    // Obtener el item de la reserva
    Long itemId = reservation.getLines().get(0).getItemId();
    ItemDetailResponse item = catalogClient.getItemDetail(itemId);
    
    // Obtener la branch del item
    Long branchId = item.getBranchId();
    BranchDetailResponse branch = catalogClient.getBranchDetail(branchId);
    
    return providerId.equals(branch.getProviderId());
}
```

#### 4️⃣ Actualizar CatalogClient con nuevos métodos

**Archivo:** `services/booking-service/src/main/java/com/domination/booking/service/CatalogClient.java`

```java
public List<Long> getBranchIdsByProviderId(Long providerId) {
    String url = catalogServiceUrl + "/api/catalog/branches?providerId=" + providerId;
    // Llamar y parsear respuesta
    // Retornar lista de IDs
}

public List<Long> getItemIdsByBranchId(Long branchId) {
    String url = catalogServiceUrl + "/api/catalog/items?branchId=" + branchId;
    // Llamar y parsear respuesta
    // Retornar lista de IDs
}

public BranchDetailResponse getBranchDetail(Long branchId) {
    String url = catalogServiceUrl + "/api/catalog/branches/" + branchId;
    return restClient.get()
            .uri(url)
            .retrieve()
            .body(BranchDetailResponse.class);
}
```

#### 5️⃣ Crear AdminReservationController

**Archivo (NUEVO):** `services/booking-service/src/main/java/com/domination/booking/controller/AdminReservationController.java`

```java
package com.domination.booking.controller;

import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {
    
    private final ReservationService reservationService;
    
    // GET /api/booking/admin/reservations - Ver todas las reservas
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationDTO>> getAllReservations() {
        List<ReservationDTO> reservations = reservationService.findAll();
        return ResponseEntity.ok(reservations);
    }
    
    // PATCH /api/booking/admin/reservations/{id}/status
    @PatchMapping("/reservations/{id}/status")
    public ResponseEntity<ReservationDTO> updateReservationStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status) {
        ReservationDTO updated = reservationService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /api/booking/admin/reservations/{id}
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### C.4) ACTUALIZAR DOCKER COMPOSE Y README

El docker-compose.yml ya está correcto. Solo actualizar el README principal con la nueva estructura de permisos.

---

## 📝 TODOS Y ROADMAP

### D.1) IMPLEMENTACIÓN INMEDIATA (Sprint 1 - Esta Semana)

- [ ] **AUTH-SERVICE**
  - [x] Agregar `ROLE_PROVIDER` al enum RoleName
  - [x] Actualizar DataSeeder para crear rol PROVIDER
  - [ ] Agregar campo `providerId` a User entity
  - [ ] Incluir `providerId` en JWT claims (OAuth2TokenService)
  - [ ] Crear endpoint `/auth/register/provider`
  - [ ] Crear endpoint `/admin/users` (CRUD usuarios)
  
- [ ] **CATALOG-SERVICE**
  - [ ] Agregar campo `providerId` a Branch entity (+ migration)
  - [ ] Actualizar SecurityConfig con `@EnableMethodSecurity`
  - [ ] Crear `ProviderController` completo (6 endpoints)
  - [ ] Agregar métodos de servicio con validación de ownership
  - [ ] Agregar `findByProviderId` al BranchRepository
  - [ ] Crear excepción `AccessDeniedException`
  - [ ] Actualizar `AdminController` con endpoints faltantes
  - [ ] Crear endpoint GET `/api/catalog/branches?providerId=X`
  
- [ ] **BOOKING-SERVICE**
  - [ ] Agregar endpoint GET `/api/booking/reservations/{id}` con validación de roles
  - [ ] Crear `ProviderReservationController`
  - [ ] Agregar método `findByProviderId` en ReservationService
  - [ ] Actualizar `CatalogClient` con métodos de provider
  - [ ] Crear `AdminReservationController`
  - [ ] Agregar método `updateStatus` en ReservationService
  - [ ] Implementar validación de ownership en `belongsToProvider`

### D.2) FUNCIONALIDADES FUTURAS (Sprint 2-3)

- [ ] **PAYMENTS-SERVICE (NUEVO MICROSERVICIO)**
  - [ ] Configurar MercadoPago SDK
  - [ ] Endpoint POST `/api/payments/reservations/{id}/pay` (Cliente)
  - [ ] Endpoint POST `/api/payments/reservations/{id}/register` (Provider - registrar pago en efectivo)
  - [ ] Webhook `/api/payments/webhook/mercadopago`
  - [ ] Integración con booking-service para actualizar estado de reserva
  
- [ ] **SOPORTE INSTRUMENTOS/ACCESORIOS**
  - [ ] Ya existe `RentalMode` (TIME_EXCLUSIVE/TIME_QUANTITY) ✅
  - [ ] Implementar validación de stock para TIME_QUANTITY
  - [ ] UI para agregar instrumentos por item
  - [ ] Filtros en frontend por tipo de item
  
- [ ] **NOTIFICACIONES**
  - [ ] Email de confirmación de reserva
  - [ ] Email de recordatorio 24h antes
  - [ ] Notificación a provider de nueva reserva
  
- [ ] **REPORTES**
  - [ ] Dashboard provider: reservas por mes, ingresos
  - [ ] Dashboard admin: métricas globales
  - [ ] Exportar a PDF/Excel

### D.3) MIGRACIONES DE DATOS

Si ya tienes datos en el proyecto viejo que quieras migrar:

```sql
-- Script de migración (ejecutar manualmente)

-- 1. Migrar usuarios de monolito a auth-service
INSERT INTO auth_db.users (username, email, password, enabled, created_at, updated_at)
SELECT 
    nombreUsuario as username,
    email,
    password,
    true as enabled,
    NOW() as created_at,
    NOW() as updated_at
FROM monolito_db.usuario;

-- 2. Asignar roles según tipo de usuario
-- (Requiere lógica custom para mapear rol String a Role ID)

-- 3. Migrar sucursales a catalog-service
INSERT INTO catalog_db.branch (name, address, active, provider_id)
SELECT 
    nombre as name,
    CONCAT(d.calle, ' ', d.altura, ', ', d.localidad) as address,
    true as active,
    s.prestador_idprestador as provider_id
FROM monolito_db.sucursal s
LEFT JOIN monolito_db.domicilio d ON s.idSucursal = d.sucursal_idsucursal;

-- 4. Migrar salas (items) a catalog-service
INSERT INTO catalog_db.rentable_item (branch_id, name, type, rental_mode, base_price, active)
SELECT 
    s.idSucursal as branch_id,
    CONCAT('Sala ', sala.nombre) as name,
    'ROOM' as type,
    'TIME_EXCLUSIVE' as rental_mode,
    sala.valorHora as base_price,
    true as active
FROM monolito_db.sala sala
JOIN monolito_db.sucursal s ON sala.sucursal_idsucursal = s.idSucursal;

-- 5. Migrar reservas a booking-service
-- (Similar pattern)
```

---

## 🎯 DECISIONES DE ARQUITECTURA Y JUSTIFICACIÓN

### 1. ¿Por qué `providerId` en JWT y no solo en DB?

**Decisión:** Incluir `providerId` como claim custom en JWT.

**Justificación:**
- **Performance**: Evita consultas adicionales a auth-service en cada request
- **Stateless**: El microservicio puede validar ownership sin llamadas externas
- **Security**: El JWT está firmado, no se puede falsificar el `providerId`

**Alternativa descartada:** Consultar auth-service en cada request → Latencia alta, single point of failure

### 2. ¿Por qué no validar JWT en el Gateway?

**Decisión:** Gateway solo rutea, cada microservicio valida JWT independientemente.

**Justificación:**
- **Separación de responsabilidades**: Cada servicio es responsable de su seguridad
- **Flexibilidad**: Diferentes servicios pueden tener diferentes reglas de validación
- **Resiliencia**: Si el Gateway falla, los servicios siguen siendo seguros

**Alternativa descartada:** Validar en Gateway → Single point of failure, menos granularidad

### 3. ¿Por qué `@PreAuthorize` en vez de SecurityFilterChain para permisos granulares?

**Decisión:** Usar `@PreAuthorize("hasRole('PROVIDER')")` en métodos de controller.

**Justificación:**
- **Legibilidad**: Cada método declara explícitamente qué roles requiere
- **Flexibilidad**: Expresiones SpEL permiten lógica compleja (ej: `hasRole('ADMIN') or #userId == principal.name`)
- **Documentación viva**: El código autodocumenta los permisos

**Alternativa descartada:** Solo SecurityFilterChain → Menos flexible, difícil mantener reglas complejas

### 4. ¿Por qué separar ProviderController y AdminController?

**Decisión:** Crear controllers separados por rol/responsabilidad.

**Justificación:**
- **Clarity**: Cada controller tiene un propósito claro
- **Seguridad**: Menor riesgo de exponer endpoints de admin a providers
- **Testing**: Más fácil testear permisos por controller

**Alternativa descartada:** Un solo controller con `if (hasRole)` → Menos claro, más propenso a errores

---

## 🔐 CHECKLIST DE SEGURIDAD

Antes de ir a producción, verificar:

- [ ] Todos los endpoints tienen `@PreAuthorize` o están en SecurityFilterChain
- [ ] No hay endpoints expuestos sin autenticación (excepto públicos intencionales)
- [ ] `providerId` se valida en TODOS los endpoints de provider
- [ ] JWT expiration está configurado (no más de 1 hora para access token)
- [ ] HTTPS habilitado en producción
- [ ] CORS restringido a dominios conocidos (no `*`)
- [ ] Logs NO contienen tokens JWT completos
- [ ] Rate limiting configurado en Gateway
- [ ] Validación de input en todos los DTOs (`@Valid`, `@NotNull`, etc.)
- [ ] SQL injection protegido por JPA (no queries nativas sin parámetros)

---

## 📚 REFERENCIAS Y CONVENCIONES

### Nomenclatura de Roles
- Prefijo `ROLE_` es requerido por Spring Security
- Usar UPPERCASE para nombres de roles (ej: `ROLE_ADMIN`, no `ROLE_Admin`)

### Endpoints REST
- Plurales para colecciones: `/branches`, `/items`, `/reservations`
- Singulares para recursos específicos: `/branches/{id}`
- Verbos HTTP estándar: GET (leer), POST (crear), PUT (actualizar completo), PATCH (actualizar parcial), DELETE (eliminar)

### HTTP Status Codes
- `200 OK`: GET, PUT, PATCH exitosos
- `201 Created`: POST exitoso (incluir header `Location`)
- `204 No Content`: DELETE exitoso
- `400 Bad Request`: Validación fallida
- `401 Unauthorized`: No autenticado (token faltante o inválido)
- `403 Forbidden`: Autenticado pero sin permisos
- `404 Not Found`: Recurso no existe
- `409 Conflict`: Conflicto (ej: reserva solapada)

---

**Fecha de Auditoría:** 2026-01-08  
**Versión:** 1.0  
**Autor:** Tech Lead / Arquitecto  
**Estado:** ✅ APROBADO PARA IMPLEMENTACIÓN

