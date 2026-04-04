# Sprint 4 - Stock real (hold/release) en catalog-service

Fecha: 2026-02-23

Este documento contiene:

1. **La última respuesta entregada** al prompt anterior.
2. **Diffs documentados** de los cambios aplicados en `catalog-service`.

---

## 1) Última respuesta al prompt anterior

Implementado Sprint 4 en `catalog-service` con stock real por hold/release, respetando packages y sin tocar `booking-service`.

### Cambios realizados

#### Nuevas entidades y persistencia (tabla `inventory_holds`)
- `services/catalog-service/src/main/java/com/domination/catalog/domain/InventoryHold.java`
  - Campos:
    - `id` (`String`, UUID)
    - `itemId`
    - `quantity`
    - `startAt`
    - `endAt`
    - `status` (`HELD`, `RELEASED`, `EXPIRED`)
    - `createdAt`
- `services/catalog-service/src/main/java/com/domination/catalog/domain/InventoryHoldStatus.java`
- `services/catalog-service/src/main/java/com/domination/catalog/repository/InventoryHoldRepository.java`
  - Query para sumar holds activos solapados por rango:
    - `sumHeldQuantityInRange(itemId, startAt, endAt, HELD)`

#### Nuevos DTOs
- `services/catalog-service/src/main/java/com/domination/catalog/dto/HoldInventoryRequest.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/HoldInventoryResponse.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/ReleaseInventoryRequest.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/ReleaseInventoryResponse.java`

#### Nuevo service
- `services/catalog-service/src/main/java/com/domination/catalog/service/InventoryHoldService.java`

`holdInventory(...)`
- Valida rango (`startAt < endAt`).
- Obtiene `quantityTotal` desde `Inventory` por `itemId`.
- Calcula `heldQty` activo en el rango (`status=HELD`).
- Regla:
  - si `requested > (totalStock - heldQty)` => conflicto.
- Persiste hold con:
  - `id = UUID`
  - `status = HELD`
  - `createdAt = now`
- Retorna:
  - `holdId`, `expiresAt` (null opcional), `itemId`, `quantity`

`releaseInventory(...)`
- Busca por `holdId`.
- Si no existe => `released=false`.
- Si existe y está en `HELD` => cambia a `RELEASED`, guarda, `released=true`.
- Si ya está `RELEASED/EXPIRED` => `released=false`.

#### Nuevo controller con endpoints pedidos
- `services/catalog-service/src/main/java/com/domination/catalog/controller/InventoryController.java`

Endpoints:
- `POST /api/catalog/inventory/hold`
- `POST /api/catalog/inventory/release`

#### Manejo de conflicto (409)
- `services/catalog-service/src/main/java/com/domination/catalog/exception/InventoryHoldConflictException.java`
- `services/catalog-service/src/main/java/com/domination/catalog/exception/GlobalExceptionHandler.java`
  - handler para devolver `409 CONFLICT` con title `"Conflicto de inventario"`.

#### Tests unitarios mínimos de service
- `services/catalog-service/src/test/java/com/domination/catalog/service/InventoryHoldServiceTest.java`

Casos cubiertos:
1. No permite hold cuando supera disponible en rango.
2. Crea hold correctamente cuando hay stock.
3. Release marca `RELEASED` cuando hold existe y está en `HELD`.
4. Release devuelve `released=false` cuando hold no existe.

No se modificó `booking-service`.
No se tocaron endpoints existentes fuera de este alcance.

---

## 2) Diffs documentados (Sprint 4)

## `GlobalExceptionHandler.java`

Se agregó manejo específico de conflicto de inventario:

```diff
@@ public class GlobalExceptionHandler {
+    @ExceptionHandler(InventoryHoldConflictException.class)
+    public ProblemDetail handleInventoryHoldConflict(InventoryHoldConflictException ex) {
+        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
+                HttpStatus.CONFLICT,
+                ex.getMessage()
+        );
+        problemDetail.setTitle("Conflicto de inventario");
+        problemDetail.setProperty("timestamp", Instant.now());
+        return problemDetail;
+    }
```

## `InventoryHoldStatus.java` (nuevo)

```diff
+public enum InventoryHoldStatus {
+    HELD,
+    RELEASED,
+    EXPIRED
+}
```

## `InventoryHold.java` (nuevo)

```diff
+@Entity
+@Table(name = "inventory_holds")
+public class InventoryHold {
+    @Id
+    @Column(nullable = false, length = 64)
+    private String id;
+
+    @Column(name = "item_id", nullable = false)
+    private Long itemId;
+
+    @Column(nullable = false)
+    private Integer quantity;
+
+    @Column(name = "start_at", nullable = false)
+    private LocalDateTime startAt;
+
+    @Column(name = "end_at", nullable = false)
+    private LocalDateTime endAt;
+
+    @Enumerated(EnumType.STRING)
+    @Column(nullable = false, length = 20)
+    private InventoryHoldStatus status;
+
+    @Column(name = "created_at", nullable = false)
+    private LocalDateTime createdAt;
+}
```

## `InventoryHoldRepository.java` (nuevo)

```diff
+public interface InventoryHoldRepository extends JpaRepository<InventoryHold, String> {
+    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM InventoryHold h " +
+           "WHERE h.itemId = :itemId " +
+           "AND h.status = :status " +
+           "AND h.startAt < :endAt " +
+           "AND h.endAt > :startAt")
+    Integer sumHeldQuantityInRange(
+            Long itemId,
+            LocalDateTime startAt,
+            LocalDateTime endAt,
+            InventoryHoldStatus status
+    );
+}
```

## DTOs nuevos

### `HoldInventoryRequest.java`
```diff
+private Long itemId;
+private Integer quantity;
+private LocalDateTime startAt;
+private LocalDateTime endAt;
+private String reference;
```

### `HoldInventoryResponse.java`
```diff
+private String holdId;
+private LocalDateTime expiresAt;
+private Long itemId;
+private Integer quantity;
```

### `ReleaseInventoryRequest.java`
```diff
+private String holdId;
```

### `ReleaseInventoryResponse.java`
```diff
+private boolean released;
```

## `InventoryHoldConflictException.java` (nuevo)

```diff
+public class InventoryHoldConflictException extends RuntimeException {
+    public InventoryHoldConflictException(String message) {
+        super(message);
+    }
+}
```

## `InventoryHoldService.java` (nuevo)

Resumen del diff funcional:

```diff
+public HoldInventoryResponse holdInventory(HoldInventoryRequest request) {
+    validateRange(startAt, endAt);
+    totalStock = inventoryRepository.findByItemId(itemId)...
+    heldQty = inventoryHoldRepository.sumHeldQuantityInRange(..., HELD);
+    if (requested > totalStock - heldQty) throw InventoryHoldConflictException
+    save(new InventoryHold(id=UUID, status=HELD, createdAt=now, ...));
+    return HoldInventoryResponse(holdId, expiresAt=null, itemId, quantity);
+}
+
+public ReleaseInventoryResponse releaseInventory(ReleaseInventoryRequest request) {
+    hold = findById(holdId);
+    if (missing) return released=false;
+    if (status != HELD) return released=false;
+    status = RELEASED; save(hold); return released=true;
+}
```

## `InventoryController.java` (nuevo)

```diff
+@RestController
+@RequestMapping("/api/catalog/inventory")
+public class InventoryController {
+    @PostMapping("/hold")
+    public ResponseEntity<HoldInventoryResponse> holdInventory(...)
+
+    @PostMapping("/release")
+    public ResponseEntity<ReleaseInventoryResponse> releaseInventory(...)
+}
```

## `InventoryHoldServiceTest.java` (nuevo)

Resumen del diff de cobertura:

```diff
+holdInventory_throwsConflict_whenRequestedExceedsAvailableInRange
+holdInventory_createsHold_whenStockIsAvailable
+releaseInventory_marksReleased_whenHoldExistsAndIsHeld
+releaseInventory_returnsFalse_whenHoldDoesNotExist
```

---

## Archivos nuevos/modificados en Sprint 4

### Nuevos
- `services/catalog-service/src/main/java/com/domination/catalog/domain/InventoryHoldStatus.java`
- `services/catalog-service/src/main/java/com/domination/catalog/domain/InventoryHold.java`
- `services/catalog-service/src/main/java/com/domination/catalog/repository/InventoryHoldRepository.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/HoldInventoryRequest.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/HoldInventoryResponse.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/ReleaseInventoryRequest.java`
- `services/catalog-service/src/main/java/com/domination/catalog/dto/ReleaseInventoryResponse.java`
- `services/catalog-service/src/main/java/com/domination/catalog/exception/InventoryHoldConflictException.java`
- `services/catalog-service/src/main/java/com/domination/catalog/service/InventoryHoldService.java`
- `services/catalog-service/src/main/java/com/domination/catalog/controller/InventoryController.java`
- `services/catalog-service/src/test/java/com/domination/catalog/service/InventoryHoldServiceTest.java`

### Modificados
- `services/catalog-service/src/main/java/com/domination/catalog/exception/GlobalExceptionHandler.java`

