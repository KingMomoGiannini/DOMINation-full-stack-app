# Sprint 4 (completar) - Integración booking-service con hold/release de catalog-service

Fecha: 2026-02-23

## Respuesta al prompt

Se implementó la integración en `booking-service` para usar stock real en `catalog-service` mediante endpoints de hold/release, sin tocar `catalog-service` y sin cambiar endpoints existentes (excepto extender `CatalogClient` y agregar `holdId` en `ReservationLine` como pediste).

### Objetivo cubierto

- Al crear reserva:
  - para líneas `TIME_QUANTITY` se solicita hold previo en catalog (`/api/catalog/inventory/hold`).
  - se guarda `holdId` en `ReservationLine`.
  - si falla un hold posterior, se liberan holds ya creados (best-effort) y se propaga error.
- Al cancelar reserva:
  - si ya estaba `CANCELLED`, retorna sin liberar (idempotencia).
  - si pasa a `CANCELLED`, libera holds de líneas con `holdId` (best-effort, con `log.warn` si falla).
- Tests unitarios:
  - `createReservation` cuando `holdInventory` falla con conflicto: no guarda reserva.
  - `cancelReservation` cuando cancela: libera por cada `holdId`; cuando ya estaba cancelada: no libera.

---

## Diffs aplicados

## 1) Nuevos modelos de hold/release en booking-service

Se agregaron en `com.domination.booking.model` (patrón actual del proyecto para requests/responses externos):

- `HoldInventoryRequest`
- `HoldInventoryResponse`
- `ReleaseInventoryRequest`
- `ReleaseInventoryResponse`

```diff
++ services/booking-service/src/main/java/com/domination/booking/model/HoldInventoryRequest.java
+ itemId, quantity, startAt, endAt, reference

++ services/booking-service/src/main/java/com/domination/booking/model/HoldInventoryResponse.java
+ holdId, expiresAt, itemId, quantity

++ services/booking-service/src/main/java/com/domination/booking/model/ReleaseInventoryRequest.java
+ holdId

++ services/booking-service/src/main/java/com/domination/booking/model/ReleaseInventoryResponse.java
+ released
```

---

## 2) `CatalogClient` extendido con hold/release

Archivo:
`services/booking-service/src/main/java/com/domination/booking/service/CatalogClient.java`

```diff
@@ imports
+ import com.domination.booking.model.HoldInventoryRequest;
+ import com.domination.booking.model.HoldInventoryResponse;
+ import com.domination.booking.model.ReleaseInventoryRequest;
+ import com.domination.booking.model.ReleaseInventoryResponse;
+ import com.domination.booking.exception.ConflictException;
+ import org.springframework.web.client.HttpClientErrorException;

@@ class CatalogClient
+ public HoldInventoryResponse holdInventory(HoldInventoryRequest request)
+ public ReleaseInventoryResponse releaseInventory(ReleaseInventoryRequest request)
+ private String extractCatalogErrorMessage(HttpClientErrorException ex, String fallback)
```

Comportamiento:

- `holdInventory(...)`
  - `POST /api/catalog/inventory/hold`
  - si recibe 409 del catalog, convierte a `ConflictException` con detalle del catalog.
- `releaseInventory(...)`
  - `POST /api/catalog/inventory/release`
  - si falla, lanza `RuntimeException` (y el caller hace best-effort).

---

## 3) `ReservationLine` ahora persiste `holdId`

Archivo:
`services/booking-service/src/main/java/com/domination/booking/domain/ReservationLine.java`

```diff
@@ class ReservationLine
+ @Column(name = "hold_id", length = 64)
+ private String holdId;
```

Impacto:

- Hibernate `ddl-auto=update` reflejará el nuevo campo en `reservation_lines`.

---

## 4) `ReservationService.createReservation` integrado con hold + compensación

Archivo:
`services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`

### Cambios principales

```diff
@@ imports
+ HoldInventoryRequest, HoldInventoryResponse, ReleaseInventoryRequest

@@ createReservation(...)
+ List<String> createdHoldIds = new ArrayList<>();
+ try {
+   processReservationLine(..., customerId, request.getBranchId(), createdHoldIds)
+ } catch (RuntimeException ex) {
+   releaseCreatedHoldsBestEffort(createdHoldIds);
+   throw ex;
+ }

@@ processReservationLine(...)
- private void processReservationLine(Reservation reservation, CreateReservationLineRequest lineReq)
+ private void processReservationLine(Reservation reservation, CreateReservationLineRequest lineReq,
+                                    String customerId, Long branchId, List<String> createdHoldIds)

@@ TIME_QUANTITY branch
+ HoldInventoryRequest holdRequest = HoldInventoryRequest.builder()
+   .itemId(itemId)
+   .quantity(requestedQty)
+   .startAt(reservation.getStartAt())
+   .endAt(reservation.getEndAt())
+   .reference("cust-" + customerId + "-branch-" + branchId)
+   .build();
+ HoldInventoryResponse holdResponse = catalogClient.holdInventory(holdRequest);
+ line.setHoldId(holdResponse.getHoldId());
+ createdHoldIds.add(holdResponse.getHoldId());
```

Notas:

- Se mantiene el flujo y validaciones existentes.
- El hold se realiza para `TIME_QUANTITY` antes de guardar la reserva.
- Si un hold falla, se liberan los ya creados (best-effort) y se propaga el error.

---

## 5) `ReservationService.cancelReservation` libera holds en transición a CANCELLED

Archivo:
`services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`

```diff
@@ cancelReservation(...)
  if (reservation.getStatus() == ReservationStatus.CANCELLED){
      return reservationMapper.toDTO(reservation);
  }
  ...
  reservation.setStatus(ReservationStatus.CANCELLED);
  Reservation saved = reservationRepository.save(reservation);
+
+ // Liberación best-effort de holds de líneas TIME_QUANTITY
+ saved.getLines().stream()
+     .map(ReservationLine::getHoldId)
+     .filter(holdId -> holdId != null && !holdId.isBlank())
+     .forEach(this::releaseHoldBestEffort);

@@ new helpers
+ private void releaseCreatedHoldsBestEffort(List<String> holdIds)
+ private void releaseHoldBestEffort(String holdId)
```

Idempotencia:

- Si ya está `CANCELLED`, retorna sin liberar holds.

---

## 6) Tests unitarios añadidos/ajustados

Archivo:
`services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceTest.java`

### Nuevos casos

```diff
+ createReservation_doesNotSave_whenCatalogHoldFailsWithConflict
+ cancelReservation_releasesEachHold_whenTransitionToCancelled
+ cancelReservation_doesNotRelease_whenAlreadyCancelled
```

### Cobertura de los requerimientos del prompt

- `createReservation` + conflicto de hold:
  - `catalogClient.holdInventory(...)` lanza `ConflictException`.
  - se verifica `reservationRepository.save(...)` nunca llamado.
- `cancelReservation`:
  - transición a `CANCELLED`: llama `releaseInventory` por cada `holdId`.
  - ya `CANCELLED`: no llama `releaseInventory` y no guarda.

---

## Archivos modificados/creados

### Creados

- `services/booking-service/src/main/java/com/domination/booking/model/HoldInventoryRequest.java`
- `services/booking-service/src/main/java/com/domination/booking/model/HoldInventoryResponse.java`
- `services/booking-service/src/main/java/com/domination/booking/model/ReleaseInventoryRequest.java`
- `services/booking-service/src/main/java/com/domination/booking/model/ReleaseInventoryResponse.java`

### Modificados

- `services/booking-service/src/main/java/com/domination/booking/service/CatalogClient.java`
- `services/booking-service/src/main/java/com/domination/booking/domain/ReservationLine.java`
- `services/booking-service/src/main/java/com/domination/booking/service/ReservationService.java`
- `services/booking-service/src/test/java/com/domination/booking/service/ReservationServiceTest.java`

---

## Validaciones finales

- No se tocó `catalog-service`.
- No se cambiaron endpoints existentes de controllers en `booking-service`.
- Se agregaron solo métodos de cliente y campo `holdId` según requerimiento.
- Linter sin errores en los archivos modificados.

