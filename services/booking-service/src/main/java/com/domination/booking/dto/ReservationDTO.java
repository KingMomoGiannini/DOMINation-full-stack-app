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
    /**
     * Vista operativa derivada a partir de hechos persistidos de asistencia.
     */
    private ReservationAttendanceStatus attendanceStatus;
    /**
     * Hecho persistido: momento en el que el prestador registró check-in.
     */
    private LocalDateTime checkedInAt;
    /**
     * Hecho persistido: momento en el que el prestador marcó no-show.
     */
    private LocalDateTime noShowMarkedAt;
    /**
     * Flag derivado para habilitar CTA de check-in en UI del prestador.
     */
    private boolean providerCheckInAllowed;
    /**
     * Motivo derivado cuando check-in no corresponde.
     */
    private ReservationProviderActionBlockReason providerCheckInBlockReason;
    /**
     * Flag derivado para habilitar CTA de no-show en UI del prestador.
     */
    private boolean providerMarkNoShowAllowed;
    /**
     * Motivo derivado cuando marcar no-show no corresponde.
     */
    private ReservationProviderActionBlockReason providerMarkNoShowBlockReason;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private List<ReservationLineDTO> lines = new ArrayList<>();
}


