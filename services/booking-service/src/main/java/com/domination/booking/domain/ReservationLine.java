package com.domination.booking.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa una línea de reserva (item reservado)
 */
@Entity
@Table(name = "reservation_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @NotNull(message = "El itemId es obligatorio")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser positiva")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "El precio es obligatorio")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}


