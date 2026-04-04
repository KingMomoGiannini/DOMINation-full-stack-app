# Sprint 3 - Refactor Availability (sin reflection)

Fecha: 2026-02-28

Este documento resume los cambios aplicados para refactorizar `checkAvailability` en `booking-service`, manteniendo consistencia con `createReservation` y sin tocar otras partes del servicio.

---

## Resultado general

- Se eliminó la lógica con reflection en `ReservationService.checkAvailability`.
- Para `TIME_QUANTITY` ahora se usa el mismo criterio de stock que `createReservation`: `itemDetail.getQuantityTotal()`.
- `reservedQty` sigue calculándose con `sumReservedQuantity(...)` y normalizando `null -> 0`.
- Se mantiene el endpoint `POST /api/booking/availability` en `ReservationController` (ya estaba agregado y correcto).
- Se ajustaron los tests unitarios para que el caso de stock insuficiente use `quantityTotal`.

---

## Diff 1: `ReservationService.java`

Archivo:  
`services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`

```diff
@@
-import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.stream.Collectors;

@@ checkAvailability(...)
-            if ("TIME_QUANTITY".equals(itemDetail.getRentalMode())) {
-                Integer availableQty = resolveAvailableQuantity(itemDetail);
+            if ("TIME_QUANTITY".equals(itemDetail.getRentalMode())) {
+                Integer totalStock = itemDetail.getQuantityTotal();
                 Integer reservedQty = reservationRepository.sumReservedQuantity(
                         itemId, request.getStartAt(), request.getEndAt(), ReservationStatus.CANCELLED
                 );
                 if (reservedQty == null) {
                     reservedQty = 0;
                 }

-                if (availableQty == null || requestedQty == null || (reservedQty + requestedQty > availableQty)) {
+                if (totalStock == null || requestedQty == null || (reservedQty + requestedQty > totalStock)) {
                     conflicts.add(AvailabilityConflictDTO.builder()
                             .itemId(itemId)
                             .reason("INSUFFICIENT_STOCK")
                             .detail(String.format("Stock insuficiente para item %d en el rango solicitado", itemId))
                             .requestedQty(requestedQty)
-                            .availableQty(availableQty)
+                            .availableQty(totalStock)
                             .reservedQty(reservedQty)
                             .build());
                 }
             }
@@
-    private Integer resolveAvailableQuantity(ItemDetailResponse itemDetail) {
-        // Soporta shape nuevo: itemDetail.getInventory().getAvailableQuantity()
-        try {
-            Method getInventory = itemDetail.getClass().getMethod("getInventory");
-            Object inventory = getInventory.invoke(itemDetail);
-            if (inventory != null) {
-                Method getAvailableQuantity = inventory.getClass().getMethod("getAvailableQuantity");
-                Object value = getAvailableQuantity.invoke(inventory);
-                if (value instanceof Integer integerValue) {
-                    return integerValue;
-                }
-            }
-        } catch (Exception ignored) {
-            // Fallback a shape actual
-        }
-
-        // Shape actual en este servicio
-        return itemDetail.getQuantityTotal();
-    }
```

### Estado final del criterio de stock (`TIME_QUANTITY`)

- `totalStock = itemDetail.getQuantityTotal()`
- `reservedQty = sumReservedQuantity(...)` (si `null`, se usa `0`)
- conflicto si `reservedQty + requestedQty > totalStock`
- `availableQty` en el conflicto se informa como `totalStock`

---

## Diff 2: `ReservationServiceCheckAvailabilityTest.java`

Archivo:  
`services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceCheckAvailabilityTest.java`

```diff
@@ checkAvailability_returnsInsufficientStockConflict_whenTimeQuantityHasNotEnoughStock
-        ItemDetailResponseWithInventory itemDetail = new ItemDetailResponseWithInventory();
-        itemDetail.setId(2L);
-        itemDetail.setActive(true);
-        itemDetail.setRentalMode("TIME_QUANTITY");
-        itemDetail.setInventory(new InventoryWithAvailableQuantity(10));
+        ItemDetailResponse itemDetail = ItemDetailResponse.builder()
+                .id(2L)
+                .active(true)
+                .rentalMode("TIME_QUANTITY")
+                .quantityTotal(10)
+                .build();

@@ (fin del archivo)
-    /**
-     * Subtipo de ItemDetailResponse para simular shape nuevo:
-     * itemDetail.getInventory().getAvailableQuantity()
-     */
-    static class ItemDetailResponseWithInventory extends ItemDetailResponse {
-        private InventoryWithAvailableQuantity inventory;
-
-        public InventoryWithAvailableQuantity getInventory() {
-            return inventory;
-        }
-
-        public void setInventory(InventoryWithAvailableQuantity inventory) {
-            this.inventory = inventory;
-        }
-    }
-
-    static class InventoryWithAvailableQuantity {
-        private final Integer availableQuantity;
-
-        InventoryWithAvailableQuantity(Integer availableQuantity) {
-            this.availableQuantity = availableQuantity;
-        }
-
-        public Integer getAvailableQuantity() {
-            return availableQuantity;
-        }
-    }
```

### Estado final de tests

- Se mantienen los 4 casos mínimos de `checkAvailability`.
- El caso `INSUFFICIENT_STOCK` ahora valida con `quantityTotal`.
- No se usa más clase auxiliar con `getInventory().getAvailableQuantity()`.

---

## Diff 3: `ReservationController.java`

Archivo:  
`services/booking-service/src/main/java/com/domination/booking/controller/ReservationController.java`

```diff
# Sin cambios en este refactor.
# El endpoint solicitado ya existe y está correcto:
#
# POST /api/booking/availability
# @PreAuthorize("hasRole('USER')")
# request: @Valid @RequestBody CreateReservationRequest
# jwt: @AuthenticationPrincipal Jwt
# customerId: String.valueOf(extractUserId(jwt))
# response: ResponseEntity.ok(reservationService.checkAvailability(...))
```

---

## Verificación rápida

- Linter: sin errores en los archivos modificados.
- Alcance respetado:
  - solo refactor de Sprint 3 Availability
  - sin cambios en endpoints existentes
  - sin cambios fuera de `booking-service` para esta tarea.

