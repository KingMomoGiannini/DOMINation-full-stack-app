# Sprint 3 - Availability Pre-check (booking-service)

Fecha: 2026-02-23

Este documento resume, con detalle, los avances implementados en los últimos 4 prompts, exclusivamente sobre `booking-service`.

---

## 1) Nuevos DTOs de disponibilidad

### Objetivo

Introducir contratos de respuesta para el pre-check de disponibilidad, sin tocar packages ni estructura existente.

### Archivos creados

- `services/booking-service/src/main/java/com/domination/booking/dto/AvailabilityResponse.java`
- `services/booking-service/src/main/java/com/domination/booking/dto/AvailabilityConflictDTO.java`

### Detalle técnico

Se respetó el package existente `com.domination.booking.dto` y el estilo Lombok ya usado en el proyecto:

- `@Getter`
- `@Setter`
- `@NoArgsConstructor`
- `@AllArgsConstructor`
- `@Builder`

#### `AvailabilityResponse`

Campos:

- `boolean available`
- `List<AvailabilityConflictDTO> conflicts`

Notas:

- `conflicts` se inicializa con `@Builder.Default` a lista vacía para evitar `null`.

#### `AvailabilityConflictDTO`

Campos:

- `Long itemId`
- `String reason`  
  Valores documentados para este sprint:
  - `OVERLAP`
  - `INSUFFICIENT_STOCK`
  - `ITEM_INACTIVE`
  - `INVALID_RANGE`
- `String detail`
- `Integer requestedQty` (nullable)
- `Integer availableQty` (nullable)
- `Integer reservedQty` (nullable)

---

## 2) Método nuevo en `ReservationService`

### Objetivo

Agregar validación previa de disponibilidad sin persistencia, separada de `createReservation`.

### Archivo modificado

- `services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`

### Método agregado

```java
AvailabilityResponse checkAvailability(CreateReservationRequest request, String customerId)
```

### Comportamiento implementado

1. **Valida rango temporal** (`startAt < endAt`):
   - Si es inválido: **no lanza excepción**.
   - Devuelve:
     - `available=false`
     - un conflicto `reason=INVALID_RANGE`.

2. **Itera cada línea** (`CreateReservationLineRequest`):
   - Obtiene `ItemDetailResponse` vía `catalogClient.getItemDetail(itemId)` (mismo patrón que `createReservation`).

3. **Item inactivo**:
   - Si `active=false`, agrega conflicto:
     - `reason=ITEM_INACTIVE`.

4. **Rental mode `TIME_EXCLUSIVE`**:
   - Consulta:
     - `reservationRepository.findOverlappingReservations(itemId, startAt, endAt, ReservationStatus.CANCELLED)`
   - Si hay resultados, agrega conflicto:
     - `reason=OVERLAP`.

5. **Rental mode `TIME_QUANTITY`**:
   - Obtiene `availableQty`.
   - Obtiene `reservedQty` con:
     - `reservationRepository.sumReservedQuantity(itemId, startAt, endAt, ReservationStatus.CANCELLED)`
   - Si `reservedQty + requestedQty > availableQty`, agrega conflicto:
     - `reason=INSUFFICIENT_STOCK`
     - completando `requestedQty`, `availableQty`, `reservedQty`.

6. **Resultado final**:
   - Si hay conflictos: `available=false`.
   - Si no hay conflictos: `available=true`.

### Restricciones cumplidas

- No se persiste nada.
- No se llama `reservationRepository.save`.
- No se modificó `createReservation`.
- Se mantuvo el estilo de logs (`info/debug/warn`) coherente con el servicio.

### Nota de compatibilidad del stock disponible

Se implementó resolución de cantidad disponible con compatibilidad de shape:

- Prioriza `itemDetail.getInventory().getAvailableQuantity()` si existe.
- Si no existe ese shape, usa fallback al modelo actual (`itemDetail.getQuantityTotal()`).

Esto permite soportar el requerimiento de Sprint 3 sin romper el contrato actual del modelo local.

---

## 3) Endpoint nuevo en `ReservationController`

### Objetivo

Exponer el pre-check por HTTP manteniendo autenticación y convenciones del controlador.

### Archivo modificado

- `services/booking-service/src/main/java/com/domination/booking/controller/ReservationController.java`

### Endpoint agregado

- `POST /api/booking/availability`

### Detalle técnico

- Anotaciones:
  - `@PostMapping("/availability")`
  - `@PreAuthorize("hasRole('USER')")`
  - `@Operation(...)` para Swagger
- Firma:
  - `@Valid @RequestBody CreateReservationRequest request`
  - `@AuthenticationPrincipal Jwt jwt`
- Extracción de `customerId`:
  - Igual que en `createReservation`:
    - `String customerId = String.valueOf(extractUserId(jwt));`
- Invocación al service:
  - `reservationService.checkAvailability(request, customerId)`
- Respuesta:
  - `ResponseEntity.ok(response)`

### Seguridad/Swagger

Se mantuvo el `@SecurityRequirement(name = "bearerAuth")` ya existente en el controller, que es el que funciona con la configuración OpenAPI vigente.

### Restricción cumplida

- No se alteraron endpoints existentes.

---

## 4) Tests unitarios para `checkAvailability`

### Objetivo

Cubrir los casos mínimos requeridos con Mockito/JUnit sin modificar tests existentes.

### Archivo creado

- `services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceCheckAvailabilityTest.java`

### Stack y estructura

- `@ExtendWith(MockitoExtension.class)`
- Mocks:
  - `ReservationRepository`
  - `CatalogClient`
  - `ReservationMapper` (consistente con tests existentes)
- `@InjectMocks ReservationService`

### Casos implementados

1. **Invalid range** (`startAt >= endAt`)
   - Esperado:
     - `available=false`
     - conflicto `INVALID_RANGE`
   - Verificación adicional:
     - sin interacciones con `CatalogClient`
     - no persistencia.

2. **TIME_EXCLUSIVE con overlap**
   - Mock:
     - `findOverlappingReservations(...)` devuelve lista no vacía
   - Esperado:
     - `available=false`
     - conflicto `OVERLAP`.

3. **TIME_QUANTITY con stock insuficiente**
   - Condición del caso:
     - `availableQuantity=10`
     - `reservedQty=8`
     - `requestedQty=5`
   - Esperado:
     - conflicto `INSUFFICIENT_STOCK`
     - `requestedQty=5`
     - `reservedQty=8`
     - `availableQty=10`.
   - Nota:
     - Para simular `itemDetail.getInventory().getAvailableQuantity()`, el test usa un subtipo local con `getInventory()`.

4. **Caso OK (sin conflictos)**
   - Escenario mixto válido:
     - item `TIME_EXCLUSIVE` sin overlap
     - item `TIME_QUANTITY` con stock suficiente
   - Esperado:
     - `available=true`
     - `conflicts` vacía.

### Restricciones cumplidas

- No se tocaron tests existentes.
- Se añadió una clase de test nueva, aislada.

---

## Archivos impactados en total

### Código productivo

- `services/booking-service/src/main/java/com/domination/booking/dto/AvailabilityResponse.java` (nuevo)
- `services/booking-service/src/main/java/com/domination/booking/dto/AvailabilityConflictDTO.java` (nuevo)
- `services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java` (método nuevo)
- `services/booking-service/src/main/java/com/domination/booking/controller/ReservationController.java` (endpoint nuevo)

### Tests

- `services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceCheckAvailabilityTest.java` (nuevo)

---

## Resultado funcional del Sprint 3 (alcance actual)

- Existe un pre-check de disponibilidad desacoplado de la creación.
- El pre-check retorna conflictos detallados por item y motivo.
- Hay endpoint protegido para consumirlo vía API (`ROLE_USER`).
- Está cubierto por unit tests en escenarios críticos y happy path.
- No hubo cambios en persistencia ni en `createReservation` en esta etapa.

