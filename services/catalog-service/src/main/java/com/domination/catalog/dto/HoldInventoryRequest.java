package com.domination.catalog.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldInventoryRequest {

    @NotNull(message = "El itemId es obligatorio")
    private Long itemId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser positiva")
    private Integer quantity;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime startAt;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime endAt;

    private String reference;
}

