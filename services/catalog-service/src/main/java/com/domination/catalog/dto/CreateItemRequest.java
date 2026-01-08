package com.domination.catalog.dto;

import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentalMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateItemRequest {
    
    @NotNull(message = "El branchId es obligatorio")
    private Long branchId;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    
    @NotNull(message = "El tipo es obligatorio")
    private ItemType type;
    
    @NotNull(message = "El modo de alquiler es obligatorio")
    private RentalMode rentalMode;
    
    @NotNull(message = "El precio base es obligatorio")
    @Positive(message = "El precio base debe ser positivo")
    private BigDecimal basePrice;
    
    @NotNull(message = "La cantidad inicial es obligatoria")
    @PositiveOrZero(message = "La cantidad inicial no puede ser negativa")
    private Integer quantityTotal;
}


