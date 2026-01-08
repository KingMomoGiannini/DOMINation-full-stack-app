package com.domination.catalog.controller;

import com.domination.catalog.domain.ItemType;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador público para consultar items
 */
@RestController
@RequestMapping("/api/catalog/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "API pública de items alquilables")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    @Operation(summary = "Listar items", description = "Endpoint público para obtener items activos con filtros opcionales")
    public ResponseEntity<List<ItemDTO>> getItems(
            @Parameter(description = "ID de sucursal (opcional)")
            @RequestParam(required = false) Long branchId,
            
            @Parameter(description = "Tipo de item (opcional)")
            @RequestParam(required = false) ItemType type
    ) {
        return ResponseEntity.ok(itemService.getItems(branchId, type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener item por ID", description = "Endpoint público para obtener un item específico")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }
}


