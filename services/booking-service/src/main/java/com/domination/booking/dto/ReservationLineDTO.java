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
    private Integer quantity;
    private BigDecimal price;
}


