package com.domination.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una reserva
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El customerId es obligatorio")
    @Column(name = "customer_id", nullable = false)
    private String customerId; // ID del usuario (del JWT)

    @NotNull(message = "El branchId es obligatorio")
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /**
     * ID del provider dueño del branch (igual a userId del provider)
     * Se obtiene del branch al crear la reserva
     */
    @Column(name = "provider_id")
    private Long providerId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
    }
}


