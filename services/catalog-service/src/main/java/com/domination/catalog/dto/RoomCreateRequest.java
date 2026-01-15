package com.domination.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequest {
    
    @NotBlank(message = "El nombre de la sala es obligatorio")
    private String name;
    
    @NotNull(message = "El precio por hora es obligatorio")
    @Positive(message = "El precio por hora debe ser positivo")
    private BigDecimal hourlyPrice;
}

