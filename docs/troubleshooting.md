# Troubleshooting - Solución de Problemas Comunes

Esta guía cubre los problemas más comunes y sus soluciones.

## Problemas de CORS

### Síntoma

```
Access to fetch at 'http://localhost:8080/api/...' from origin 'http://localhost:5173' 
has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present
```

### Causa

El gateway no está configurando correctamente los headers CORS. Este era un problema común que causaba "Failed to fetch" en el frontend.

### Solución Implementada

**Configuración en `gateway/src/main/resources/application.properties`**:

```properties
# CORS global configurado vía properties (más simple que bean Java)
spring.cloud.gateway.globalcors.add-to-simple-url-handler-mapping=true
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins=http://localhost:5173,http://127.0.0.1:5173
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods=GET,POST,PUT,PATCH,DELETE,OPTIONS
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders=Authorization,Content-Type,Accept,Origin,X-Requested-With
spring.cloud.gateway.globalcors.corsConfigurations.[/**].exposedHeaders=Location
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowCredentials=true
spring.cloud.gateway.globalcors.corsConfigurations.[/**].maxAge=3600

# Evitar duplicación de headers CORS
spring.cloud.gateway.default-filters[0]=DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_FIRST
spring.cloud.gateway.default-filters[1]=DedupeResponseHeader=Vary, RETAIN_FIRST
```

**Nota**: El `CorsConfig.java` está comentado porque la configuración se hace vía properties (más simple y mantenible). Si necesitas usar el bean Java, descomenta y elimina la config de properties.

**Verificar que el frontend use la URL correcta**:

```env
# frontend/.env
VITE_API_BASE_URL=http://localhost:8080
```

**Reiniciar gateway**:

```bash
docker-compose restart gateway
```

### Qué Causaba el Problema

1. **Headers CORS faltantes**: El gateway no agregaba los headers necesarios en las respuestas
2. **Orígenes no permitidos**: El frontend (localhost:5173) no estaba en la lista de orígenes permitidos
3. **Headers no expuestos**: El header `Authorization` no estaba en la lista de headers permitidos

### La Solución

- Configuración CORS global en el gateway vía `application.properties`
- Orígenes explícitos: `localhost:5173` y `127.0.0.1:5173`
- Headers permitidos: `Authorization`, `Content-Type`, `Accept`, etc.
- `allowCredentials=true` para permitir cookies/JWT
- Filtros para evitar duplicación de headers CORS

## Error "Failed to fetch"

### Síntoma

En el frontend aparece "Failed to fetch" al hacer peticiones al API.

### Causas Comunes

1. **Gateway no está corriendo**
2. **URL incorrecta en frontend**
3. **CORS mal configurado** (ver sección anterior)
4. **Network error** (servicio inaccesible)

### Solución

1. **Verificar que el gateway esté corriendo**:
   ```bash
   docker-compose ps gateway
   curl http://localhost:8080/actuator/health
   ```

2. **Verificar URL en frontend**:
   - Abrir DevTools (F12) → Network
   - Ver la URL de la petición fallida
   - Debe ser `http://localhost:8080/api/...`

3. **Verificar CORS** (ver sección anterior)

4. **Ver logs del gateway**:
   ```bash
   docker-compose logs -f gateway
   ```

## Error de Autenticación JWT

### Síntoma

```
401 Unauthorized
Invalid JWT token
```

### Causas

1. Token expirado (1 hora de validez)
2. Token inválido o malformado
3. Auth service no accesible
4. JWKS endpoint no disponible

### Solución

1. **Verificar que auth-service esté corriendo**:
   ```bash
   curl http://localhost:9000/actuator/health
   ```

2. **Verificar JWKS endpoint**:
   ```bash
   curl http://localhost:9000/oauth2/jwks
   ```

3. **Verificar issuer-uri en servicios**:
   ```properties
   # catalog-service y booking-service
   spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-service:9000
   ```

4. **Obtener nuevo token**:
   - Hacer login nuevamente desde el frontend
   - O usar curl:
     ```bash
     curl -X POST http://localhost:9000/auth/login \
       -H "Content-Type: application/json" \
       -d '{"username":"adminSeba","password":"123456admin"}'
     ```

## Error de Conexión a Base de Datos

### Síntoma

```
org.postgresql.util.PSQLException: Connection refused
```

### Causa

PostgreSQL no está corriendo o no es accesible.

### Solución

1. **Verificar que PostgreSQL esté corriendo**:
   ```bash
   docker-compose ps postgres-catalog
   docker-compose ps postgres-booking
   docker-compose ps postgres-auth
   ```

2. **Verificar health checks**:
   ```bash
   docker-compose logs postgres-catalog
   ```

3. **Verificar variables de entorno**:
   ```bash
   docker-compose exec catalog-service env | grep DATASOURCE
   ```

4. **Reiniciar base de datos**:
   ```bash
   docker-compose restart postgres-catalog
   ```

## Request-Id no aparece en logs

### Síntoma

Los logs no incluyen el `[requestId]` en el formato.

### Causa

El filtro RequestIdMdcFilter no está configurado o el patrón de logging no incluye MDC.

### Solución

1. **Verificar patrón de logging en `application.properties`**:
   ```properties
   logging.pattern.console=%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%X{requestId}] %logger{36} - %msg%n
   ```
   El `%X{requestId}` es el que incluye el requestId del MDC.

2. **Verificar que el filtro esté registrado**:
   - En gateway: `RequestIdFilter` debe estar como `@Component` (clase: `gateway/src/main/java/com/domination/gateway/config/RequestIdFilter.java`)
   - En servicios: `RequestIdMdcFilter` debe estar como `@Component` (en cada servicio)

3. **Verificar que el header se propague**:
   ```bash
   curl -H "X-Request-Id: test-123" http://localhost:8080/api/catalog/branches -v
   ```
   Debe aparecer `X-Request-Id: test-123` en la respuesta.

### Cómo Funciona Request-Id Tracing

1. **Gateway genera Request-Id**:
   - Si la petición ya tiene `X-Request-Id`, lo usa
   - Si no, genera un UUID nuevo
   - Lo agrega al header de la petición que se reenvía al servicio

2. **Servicios reciben y propagan**:
   - Cada servicio tiene un `RequestIdMdcFilter` que:
     - Lee el header `X-Request-Id` de la petición
     - Si no existe, genera uno nuevo
     - Lo pone en MDC con la clave `requestId`
     - Lo agrega al header de respuesta

3. **Logs incluyen Request-Id**:
   - El patrón de logging incluye `%X{requestId}`
   - Todos los logs de esa petición incluyen el mismo ID
   - Facilita correlación de logs entre servicios

**Ejemplo de log con Request-Id**:
```
2024-01-15T10:30:45.123Z INFO [a1b2c3d4-e5f6-7890-abcd-ef1234567890] com.domination.catalog.controller.BranchController - Listing all branches
```

Este mismo ID aparecerá en todos los logs relacionados con esa petición, incluso si pasa por múltiples servicios.

## Servicios no inician en Docker

### Síntoma

```
ERROR: for service  Service "xxx" failed to start
```

### Causas

1. Puerto ya en uso
2. Dependencias no satisfechas
3. Error en build de imagen
4. Variables de entorno incorrectas

### Solución

1. **Verificar puertos**:
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```

2. **Ver logs del servicio**:
   ```bash
   docker-compose logs service-name
   ```

3. **Reconstruir imagen**:
   ```bash
   docker-compose build --no-cache service-name
   docker-compose up -d service-name
   ```

4. **Verificar dependencias**:
   ```bash
   docker-compose ps
   ```
   Todos los servicios dependientes deben estar `Up`.

## Prometheus no scrapea servicios

### Síntoma

En http://localhost:9090/targets aparecen targets en estado `DOWN`.

### Causa

1. Servicios no accesibles desde Prometheus
2. Endpoint `/actuator/prometheus` no disponible
3. Configuración incorrecta en `prometheus.yml`

### Solución

1. **Verificar que los servicios estén corriendo**:
   ```bash
   docker-compose ps
   ```

2. **Verificar endpoint desde Prometheus**:
   ```bash
   docker-compose exec prometheus wget -O- http://gateway:8080/actuator/prometheus
   ```

3. **Verificar configuración en `prometheus.yml`**:
   - Los targets deben usar nombres de host internos de Docker
   - El `metrics_path` debe ser `/actuator/prometheus`

4. **Reiniciar Prometheus**:
   ```bash
   docker-compose restart prometheus
   ```

## Frontend no se conecta al API

### Síntoma

El frontend muestra errores de conexión o timeouts.

### Causa

1. Gateway no está corriendo
2. URL incorrecta en `.env`
3. CORS mal configurado

### Solución

1. **Verificar que el gateway responda**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **Verificar `.env` en frontend**:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   ```

3. **Reiniciar frontend** (Vite necesita reiniciar para leer `.env`):
   ```bash
   # Detener (Ctrl+C) y volver a iniciar
   npm run dev
   ```

4. **Verificar en DevTools**:
   - Network tab → Ver la URL de la petición
   - Console tab → Ver errores de CORS

## Errores 500 en servicios

### Síntoma

Las peticiones retornan `500 Internal Server Error`.

### Causa

Error en el código del servicio o configuración incorrecta.

### Solución

1. **Ver logs del servicio**:
   ```bash
   docker-compose logs -f service-name
   ```

2. **Verificar stack trace**:
   - Buscar `Exception` o `Error` en los logs
   - Identificar la causa raíz

3. **Verificar configuración**:
   - Variables de entorno
   - `application.properties`
   - Conexión a base de datos

4. **Verificar datos**:
   - Si el error es al crear/actualizar, verificar que los datos sean válidos
   - Verificar constraints de base de datos

## Limpiar y Empezar de Nuevo

Si nada funciona, limpia todo y empieza de nuevo:

```bash
# Detener y eliminar contenedores, redes y volúmenes
docker-compose down -v

# Eliminar imágenes
docker image prune -a

# Reconstruir desde cero
docker-compose build --no-cache
docker-compose up -d

# Ver logs
docker-compose logs -f
```

## Comandos Útiles de Debugging

```bash
# Ver estado de todos los servicios
docker-compose ps

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker-compose logs -f gateway

# Ejecutar comando en un contenedor
docker-compose exec gateway sh

# Ver uso de recursos
docker stats

# Ver red Docker
docker network inspect domination-network

# Ver volúmenes
docker volume ls

# Limpiar recursos no usados
docker system prune -a
```

## Swagger UI: "Failed to load remote configuration"

### Síntoma

Swagger UI (http://localhost:8082/swagger-ui.html) abre pero muestra "Failed to load remote configuration" o no carga la spec.

### Causa

El endpoint `/api-docs` devuelve **401** porque está protegido por Spring Security.

### Solución

En el **booking-service**, en `SecurityConfig`, permitir explícitamente:

- `/swagger-ui/**`
- `/swagger-ui.html`
- `/api-docs`
- `/api-docs/**`
- `/v3/api-docs/**` (por compatibilidad)

Resultado: `/api-docs` responde 200 y Swagger UI carga correctamente.

## Swagger: Authorize no aparece / no puedo pegar token

### Síntoma

El candado en Swagger no abre modal o no permite pegar el token JWT.

### Causa

El OpenAPI no declara un `SecurityScheme` Bearer (JWT).

### Solución

Agregar configuración OpenAPI con un `SecurityScheme` tipo HTTP Bearer (JWT) para habilitar "Authorize". Ejemplo (Java):

```java
@Bean
public OpenAPI customOpenAPI() {
  return new OpenAPI()
    .components(new Components()
      .addSecuritySchemes("bearer-jwt",
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
}
```

## Swagger: Authorize aplicado pero requests siguen 401 / anonymous

### Síntoma

Se pega el token en Authorize pero las peticiones siguen retornando 401 o "anonymous".

### Causa

El nombre del `SecurityRequirement` en el controller no coincide con el nombre del scheme declarado en OpenAPI.

### Solución

Alinear el nombre del `@SecurityRequirement` del controller con el nombre del scheme. Ejemplo:

- Scheme: `bearer-jwt`
- Controller: `@SecurityRequirement(name = "bearer-jwt")`

Si el nombre no coincide, Swagger no envía el header `Authorization: Bearer <token>`.

## Obtener Ayuda

Si el problema persiste:

1. **Recopilar información**:
   - Logs de todos los servicios: `docker-compose logs > logs.txt`
   - Estado de servicios: `docker-compose ps`
   - Configuración relevante

2. **Verificar documentación**:
   - [Arquitectura](./arquitectura.md)
   - [Docker Compose](./docker-compose.md)
   - [Observabilidad](./observabilidad.md)

3. **Buscar en issues** (si es un proyecto open source)

4. **Crear issue** con:
   - Descripción del problema
   - Pasos para reproducir
   - Logs relevantes
   - Configuración (sin datos sensibles)

---

## Changelog

### 2025-02-23

- Swagger UI: "Failed to load remote configuration" (401 en /api-docs) → permitir en SecurityConfig.
- Swagger Authorize no aparece → falta SecurityScheme bearer JWT en OpenAPI.
- Swagger autoriza pero requests 401/anonymous → mismatch nombre SecurityRequirement vs SecurityScheme.
