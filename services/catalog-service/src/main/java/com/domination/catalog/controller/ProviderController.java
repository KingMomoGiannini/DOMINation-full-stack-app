package com.domination.catalog.controller;

import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.dto.RoomCreateRequest;
import com.domination.catalog.dto.UpdateActiveRequest;
import com.domination.catalog.service.BranchService;
import com.domination.catalog.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para endpoints de providers (prestadores)
 * Permite a los providers gestionar sus propias sucursales
 */
@RestController
@RequestMapping("/api/catalog/provider")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class ProviderController {
    
    private final BranchService branchService;
    private final ItemService itemService;
    
    /**
     * GET /api/catalog/provider/branches
     * Obtiene todas las sucursales del provider autenticado
     */
    @GetMapping("/branches")
    public ResponseEntity<List<BranchDTO>> getMyBranches(
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} consultando sus sucursales", providerId);
        
        List<BranchDTO> branches = branchService.findByProviderId(providerId);
        return ResponseEntity.ok(branches);
    }
    
    /**
     * POST /api/catalog/provider/branches
     * Crea una nueva sucursal para el provider autenticado
     */
    @PostMapping("/branches")
    public ResponseEntity<BranchDTO> createBranch(
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} creando sucursal: {}", providerId, request.getName());
        
        BranchDTO created = branchService.createForProvider(request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * PUT /api/catalog/provider/branches/{id}
     * Actualiza una sucursal propia del provider
     */
    @PutMapping("/branches/{id}")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} actualizando sucursal {}", providerId, id);
        
        BranchDTO updated = branchService.updateForProvider(id, request, providerId);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * DELETE /api/catalog/provider/branches/{id}
     * Elimina una sucursal propia del provider
     */
    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Void> deleteBranch(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} eliminando sucursal {}", providerId, id);
        
        branchService.deleteForProvider(id, providerId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * PATCH /api/catalog/provider/branches/{id}/active
     * Habilita o inhabilita una sucursal propia del provider
     */
    @PatchMapping("/branches/{id}/active")
    public ResponseEntity<BranchDTO> updateBranchActive(
            @PathVariable Long id,
            @Valid @RequestBody UpdateActiveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} {} sucursal {}", providerId, request.getActive() ? "habilitando" : "inhabilitando", id);
        
        BranchDTO updated = branchService.setActiveForProvider(id, request.getActive(), providerId);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/catalog/provider/branches/{branchId}/rooms
     * Crea una sala en una sucursal propia del provider
     */
    @PostMapping("/branches/{branchId}/rooms")
    public ResponseEntity<ItemDTO> createRoom(
            @PathVariable Long branchId,
            @Valid @RequestBody RoomCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractProviderId(jwt);
        log.info("Provider {} creando sala en sucursal {}", providerId, branchId);
        
        ItemDTO created = itemService.createRoomForProvider(branchId, request, providerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Helper: Extrae el userId del JWT y lo usa como providerId
     * El providerId ES el userId del usuario con rol PROVIDER
     */
    private Long extractProviderId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new IllegalStateException("userId no encontrado en JWT");
        }
        
        // Convertir a Long (puede venir como Integer o Long dependiendo del JSON)
        if (userIdClaim instanceof Integer) {
            return ((Integer) userIdClaim).longValue();
        }
        return (Long) userIdClaim;
    }
}

