package com.domination.booking.model;

import lombok.*;

/**
 * Respuesta del catalog-service al consultar inventario
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private Long branchId;
    private Long itemId;
    private Integer quantityTotal;
}


