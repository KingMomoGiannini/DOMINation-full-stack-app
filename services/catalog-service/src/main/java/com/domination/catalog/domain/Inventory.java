package com.domination.catalog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

/**
 * Entidad que representa el inventario de items por sucursal
 */
@Entity
@Table(name = "inventory", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"branch_id", "item_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El branchId es obligatorio")
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @NotNull(message = "El itemId es obligatorio")
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotNull(message = "La cantidad total es obligatoria")
    @PositiveOrZero(message = "La cantidad total no puede ser negativa")
    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;
}


