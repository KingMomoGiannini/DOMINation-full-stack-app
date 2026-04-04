package com.domination.booking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationLineDTO {
    private Long id;
    private Long itemId;
    /**
     * Nombre del ítem (snapshot al reservar o rellenado al leer si faltaba).
     */
    private String itemName;
    private Integer quantity;
    private BigDecimal price;
}


