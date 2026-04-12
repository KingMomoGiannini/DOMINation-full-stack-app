package com.domination.booking.dto;

import com.domination.booking.domain.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDTO {
    private Long id;
    private String customerId;
    /**
     * Nombre de usuario del cliente (solo en contexto provider; resuelto vía auth-service con JWT del prestador).
     */
    private String customerUsername;
    private Long branchId;
    /**
     * Nombre legible de la sucursal (persistido al crear o rellenado al leer si faltaba en datos legacy).
     */
    private String branchName;
    private Long providerId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus status;
    /**
     * Vista operativa derivada a partir de status + tiempo actual.
     * Se usa para UX y filtros sin mezclar semántica persistida con cálculo en cliente.
     */
    private ReservationOperationalStatus operationalStatus;
    /**
     * Flag derivado con la regla vigente de cancelación.
     */
    private boolean cancellable;
    /**
     * Motivo derivado cuando la reserva ya no puede cancelarse.
     */
    private ReservationCancellationBlockReason cancellationBlockReason;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private List<ReservationLineDTO> lines = new ArrayList<>();
}


