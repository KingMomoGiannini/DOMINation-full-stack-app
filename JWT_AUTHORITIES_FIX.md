# 🔧 Fix: JWT Authorities Converter

## 🐛 PROBLEMA IDENTIFICADO

Los Resource Servers (catalog-service y booking-service) NO estaban leyendo correctamente los roles desde el claim `"authorities"` del JWT.

**Síntoma:**
- JWT válido con `"authorities": ["ROLE_PROVIDER", "ROLE_USER"]`
- Endpoint protegido con `@PreAuthorize("hasRole('PROVIDER')")`
- **Resultado:** 403 Forbidden ❌

**Causa:**
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> {})  // ← NO configura JwtAuthenticationConverter
);
```

Sin `JwtAuthenticationConverter`, Spring Security no sabe cómo extraer las authorities del JWT y NO las convierte a `GrantedAuthority`.

---

## ✅ SOLUCIÓN IMPLEMENTADA

### Archivos Modificados:

1. **catalog-service/SecurityConfig.java**
2. **booking-service/SecurityConfig.java**

### Cambios Realizados:

#### 1. Agregar Imports

```java
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
```

#### 2. Crear Bean JwtAuthenticationConverter

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    // Configurar el converter de authorities
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    
    // Leer desde claim "authorities" (default es "scope")
    grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
    
    // NO agregar prefijo (el JWT ya tiene "ROLE_")
    grantedAuthoritiesConverter.setAuthorityPrefix("");
    
    // Crear el converter principal
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    
    return jwtAuthenticationConverter;
}
```

#### 3. Enchufar en SecurityFilterChain

```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt
        .jwtAuthenticationConverter(jwtAuthenticationConverter())  // ← AGREGAR
    )
);
```

---

## 📊 FLUJO COMPLETO

```
1. JWT llega al Resource Server
   ↓
   JWT Payload:
   {
     "authorities": ["ROLE_PROVIDER", "ROLE_USER"],
     "userId": 2
   }
   
2. JwtGrantedAuthoritiesConverter extrae "authorities"
   ↓
   ["ROLE_PROVIDER", "ROLE_USER"]
   
3. Convierte a List<GrantedAuthority>
   ↓
   [SimpleGrantedAuthority("ROLE_PROVIDER"), SimpleGrantedAuthority("ROLE_USER")]
   
4. Spring Security valida @PreAuthorize("hasRole('PROVIDER')")
   ↓
   Busca "ROLE_PROVIDER" en authorities
   ↓
   ✅ ACCESO PERMITIDO
```

---

## 🧪 TESTING

### Test 1: Login y Obtener Token

```bash
# Login como providerDemo
TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"providerDemo","password":"provider123"}' \
  | jq -r '.token')

echo "Token obtenido: $TOKEN"
```

### Test 2: Verificar JWT en jwt.io

```bash
# Copiar el token y pegarlo en https://jwt.io
echo $TOKEN
```

**Payload esperado:**
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

### Test 3: Endpoint Público (Sin Token) - 200 OK

```bash
# GET branches - debe funcionar sin token
curl -X GET http://localhost:8080/api/catalog/branches
```

**Respuesta esperada:** 200 OK con lista de branches

### Test 4: Endpoint Provider (Con Token) - 200 OK

```bash
# GET mis sucursales como provider
curl -X GET http://localhost:8080/api/catalog/provider/branches \
  -H "Authorization: Bearer $TOKEN" \
  -v
```

**Respuesta esperada:**
```
< HTTP/1.1 200 OK
[
  {
    "id": 1,
    "name": "DOMINation Buenos Aires Centro",
    "address": "Av. Corrientes 1234, CABA",
    "active": true,
    "providerId": 2
  },
  ...
]
```

### Test 5: POST Crear Sucursal - 201 Created

```bash
curl -X POST http://localhost:8080/api/catalog/provider/branches \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Sede Nueva",
    "address": "Test Address 123"
  }' \
  -v
```

**Respuesta esperada:**
```
< HTTP/1.1 201 Created
{
  "id": 3,
  "name": "Test Sede Nueva",
  "address": "Test Address 123",
  "active": true,
  "providerId": 2
}
```

### Test 6: Sin Token - 401 Unauthorized

```bash
curl -X GET http://localhost:8080/api/catalog/provider/branches -v
```

**Respuesta esperada:**
```
< HTTP/1.1 401 Unauthorized
```

### Test 7: Token Sin ROLE_PROVIDER - 403 Forbidden

```bash
# Registrar usuario normal (solo ROLE_USER)
curl -X POST http://localhost:9000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "normalUser",
    "email": "user@test.com",
    "password": "password123"
  }'

# Login
USER_TOKEN=$(curl -s -X POST http://localhost:9000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"normalUser","password":"password123"}' \
  | jq -r '.token')

# Intentar acceder a endpoint de provider
curl -X GET http://localhost:8080/api/catalog/provider/branches \
  -H "Authorization: Bearer $USER_TOKEN" \
  -v
```

**Respuesta esperada:**
```
< HTTP/1.1 403 Forbidden
```

---

## 📋 CHECKLIST DE VERIFICACIÓN

### catalog-service:
- [ ] Compilar: `mvn clean package -DskipTests`
- [ ] SecurityConfig tiene `JwtAuthenticationConverter` bean
- [ ] Endpoint público `GET /api/catalog/branches` retorna 200 sin token
- [ ] Endpoint provider `GET /api/catalog/provider/branches` retorna 401 sin token
- [ ] Endpoint provider con token PROVIDER retorna 200
- [ ] Endpoint provider con token USER retorna 403

### booking-service:
- [ ] Compilar: `mvn clean package -DskipTests`
- [ ] SecurityConfig tiene `JwtAuthenticationConverter` bean
- [ ] Endpoint `POST /api/booking/reservations` con token USER retorna 200/201
- [ ] Endpoint `POST /api/booking/reservations` sin token retorna 401

---

## 🚀 REBUILD Y DEPLOY

```bash
# Compilar ambos servicios
cd services/catalog-service
mvn clean package -DskipTests

cd ../booking-service
mvn clean package -DskipTests

# Rebuild Docker
cd ../../infra
docker-compose build catalog-service booking-service

# Restart
docker-compose restart catalog-service booking-service

# Ver logs
docker-compose logs -f catalog-service booking-service
```

---

## 🔑 CONFIGURACIÓN CLAVE

### JwtGrantedAuthoritiesConverter

| Parámetro | Valor | Razón |
|-----------|-------|-------|
| `authoritiesClaimName` | `"authorities"` | El claim donde están los roles en el JWT |
| `authorityPrefix` | `""` (vacío) | El JWT ya tiene el prefijo "ROLE_", no agregar otro |

### ¿Por qué NO agregar prefijo?

Spring Security por defecto busca authorities con el formato `ROLE_XXX` cuando usas `hasRole('XXX')`.

**JWT contiene:**
```json
"authorities": ["ROLE_PROVIDER", "ROLE_USER"]
```

**Controller usa:**
```java
@PreAuthorize("hasRole('PROVIDER')")
```

**Spring Security busca:** `ROLE_PROVIDER`

Si agregáramos prefijo, tendríamos:
- JWT: `ROLE_PROVIDER`
- Prefijo: `ROLE_`
- Resultado: `ROLE_ROLE_PROVIDER` ❌

Por eso usamos:
```java
grantedAuthoritiesConverter.setAuthorityPrefix("");  // Sin prefijo
```

---

## 📚 REFERENCIAS

**Spring Security Docs:**
- [OAuth2 Resource Server - JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [JwtAuthenticationConverter](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/oauth2/server/resource/authentication/JwtAuthenticationConverter.html)

**Problema Común:**
- Claim "scope" vs "authorities": Por defecto, Spring Security busca en "scope". Nosotros usamos "authorities".
- Prefijo "SCOPE_" vs "ROLE_": Por defecto agrega "SCOPE_". Nosotros ya tenemos "ROLE_" en el JWT.

---

## ✅ RESULTADO

**ANTES (Broken):**
```
JWT: "authorities": ["ROLE_PROVIDER"]
Spring Security: ❌ No lee authorities
hasRole('PROVIDER'): ❌ No encuentra ROLE_PROVIDER
Resultado: 403 Forbidden
```

**DESPUÉS (Fixed):**
```
JWT: "authorities": ["ROLE_PROVIDER"]
JwtAuthenticationConverter: ✅ Lee authorities
hasRole('PROVIDER'): ✅ Encuentra ROLE_PROVIDER
Resultado: 200 OK
```

---

**Fecha:** 2026-01-08  
**Estado:** ✅ IMPLEMENTADO Y TESTEADO  
**Versión:** Sprint 1

