package com.domination.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {
    
    @NotNull(message = "El branchId es obligatorio")
    private Long branchId;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser futura")
    private LocalDateTime startAt;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser futura")
    private LocalDateTime endAt;
    
    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<CreateReservationLineRequest> lines;
}


