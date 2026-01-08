package com.domination.catalog.controller;

import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.dto.CreateItemRequest;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.service.BranchService;
import com.domination.catalog.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador protegido para administradores
 * Requiere JWT con rol ADMIN
 */
@RestController
@RequestMapping("/api/catalog/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "API de administración (requiere JWT + ROLE_ADMIN)")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final BranchService branchService;
    private final ItemService itemService;

    @PostMapping("/branches")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear sucursal", description = "Endpoint protegido para crear una nueva sucursal")
    public ResponseEntity<BranchDTO> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        BranchDTO created = branchService.createBranch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear item", description = "Endpoint protegido para crear un nuevo item")
    public ResponseEntity<ItemDTO> createItem(@Valid @RequestBody CreateItemRequest request) {
        ItemDTO created = itemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}


