package com.domination.catalog.controller;

import com.domination.catalog.dto.HoldInventoryRequest;
import com.domination.catalog.dto.HoldInventoryResponse;
import com.domination.catalog.dto.ReleaseInventoryRequest;
import com.domination.catalog.dto.ReleaseInventoryResponse;
import com.domination.catalog.service.InventoryHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryHoldService inventoryHoldService;

    @PostMapping("/hold")
    public ResponseEntity<HoldInventoryResponse> holdInventory(@Valid @RequestBody HoldInventoryRequest request) {
        log.info("Solicitud hold inventory: itemId={}, quantity={}", request.getItemId(), request.getQuantity());
        HoldInventoryResponse response = inventoryHoldService.holdInventory(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    public ResponseEntity<ReleaseInventoryResponse> releaseInventory(@Valid @RequestBody ReleaseInventoryRequest request) {
        log.info("Solicitud release inventory: holdId={}", request.getHoldId());
        ReleaseInventoryResponse response = inventoryHoldService.releaseInventory(request);
        return ResponseEntity.ok(response);
    }
}

