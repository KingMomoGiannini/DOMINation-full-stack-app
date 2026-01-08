package com.domination.catalog.dto;

import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentalMode;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDTO {
    private Long id;
    private Long branchId;
    private String name;
    private ItemType type;
    private RentalMode rentalMode;
    private BigDecimal basePrice;
    private Boolean active;
    private Integer quantityTotal; // desde inventory
}


