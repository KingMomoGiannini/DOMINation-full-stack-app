package com.domination.catalog.service;

import com.domination.catalog.domain.InventoryHold;
import com.domination.catalog.domain.InventoryHoldStatus;
import com.domination.catalog.dto.HoldInventoryRequest;
import com.domination.catalog.dto.HoldInventoryResponse;
import com.domination.catalog.dto.ReleaseInventoryRequest;
import com.domination.catalog.dto.ReleaseInventoryResponse;
import com.domination.catalog.exception.InventoryHoldConflictException;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.repository.InventoryHoldRepository;
import com.domination.catalog.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryHoldService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHoldRepository inventoryHoldRepository;

    @Transactional
    public HoldInventoryResponse holdInventory(HoldInventoryRequest request) {
        validateRange(request.getStartAt(), request.getEndAt());

        Integer totalStock = inventoryRepository.findByItemId(request.getItemId())
                .map(inv -> inv.getQuantityTotal())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró inventario para itemId: " + request.getItemId()));

        Integer heldQty = inventoryHoldRepository.sumHeldQuantityInRange(
                request.getItemId(),
                request.getStartAt(),
                request.getEndAt(),
                InventoryHoldStatus.HELD
        );
        if (heldQty == null) {
            heldQty = 0;
        }

        int availableQty = totalStock - heldQty;
        log.debug("Hold inventory itemId={}, requested={}, total={}, held={}, available={}",
                request.getItemId(), request.getQuantity(), totalStock, heldQty, availableQty);

        if (request.getQuantity() > availableQty) {
            throw new InventoryHoldConflictException(String.format(
                    "Stock insuficiente para item %d. Disponible: %d, solicitado: %d",
                    request.getItemId(), availableQty, request.getQuantity()
            ));
        }

        String holdId = UUID.randomUUID().toString();
        InventoryHold hold = InventoryHold.builder()
                .id(holdId)
                .itemId(request.getItemId())
                .quantity(request.getQuantity())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(InventoryHoldStatus.HELD)
                .createdAt(LocalDateTime.now())
                .build();

        inventoryHoldRepository.save(hold);
        log.info("Hold creado: holdId={}, itemId={}, quantity={}",
                hold.getId(), hold.getItemId(), hold.getQuantity());

        return HoldInventoryResponse.builder()
                .holdId(hold.getId())
                .expiresAt(null)
                .itemId(hold.getItemId())
                .quantity(hold.getQuantity())
                .build();
    }

    @Transactional
    public ReleaseInventoryResponse releaseInventory(ReleaseInventoryRequest request) {
        var holdOpt = inventoryHoldRepository.findById(request.getHoldId());
        if (holdOpt.isEmpty()) {
            log.debug("Release no-op: holdId {} no encontrado", request.getHoldId());
            return ReleaseInventoryResponse.builder().released(false).build();
        }

        InventoryHold hold = holdOpt.get();
        if (hold.getStatus() != InventoryHoldStatus.HELD) {
            log.debug("Release no-op: holdId {} en estado {}", hold.getId(), hold.getStatus());
            return ReleaseInventoryResponse.builder().released(false).build();
        }

        hold.setStatus(InventoryHoldStatus.RELEASED);
        inventoryHoldRepository.save(hold);
        log.info("Hold liberado: holdId={}", hold.getId());

        return ReleaseInventoryResponse.builder().released(true).build();
    }

    private void validateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }
}

