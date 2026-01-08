package com.domination.catalog.controller;

import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.service.BranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador público para consultar sucursales
 */
@RestController
@RequestMapping("/api/catalog/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "API pública de sucursales")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Listar sucursales activas", description = "Endpoint público para obtener todas las sucursales activas")
    public ResponseEntity<List<BranchDTO>> getAllBranches() {
        return ResponseEntity.ok(branchService.getAllActiveBranches());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sucursal por ID", description = "Endpoint público para obtener una sucursal específica")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }
}


