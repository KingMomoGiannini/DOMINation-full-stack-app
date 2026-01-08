package com.domination.booking.model;

import lombok.*;

import java.math.BigDecimal;

/**
 * Respuesta del catalog-service al consultar un item
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDetailResponse {
    private Long id;
    private Long branchId;
    private String name;
    private String type;          // ROOM, INSTRUMENT, etc.
    private String rentalMode;    // TIME_EXCLUSIVE, TIME_QUANTITY
    private BigDecimal basePrice;
    private Boolean active;
    private Integer quantityTotal;
}


