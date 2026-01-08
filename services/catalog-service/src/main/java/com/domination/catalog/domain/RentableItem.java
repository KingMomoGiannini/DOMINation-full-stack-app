package com.domination.catalog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa un item alquilable (sala, instrumento, accesorio, etc.)
 */
@Entity
@Table(name = "rentable_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El branchId es obligatorio")
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    @NotNull(message = "El modo de alquiler es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rental_mode", nullable = false, length = 20)
    private RentalMode rentalMode;

    @NotNull(message = "El precio base es obligatorio")
    @Positive(message = "El precio base debe ser positivo")
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Boolean active = true;
}


