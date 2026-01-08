package com.domination.catalog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Entidad que representa una sucursal
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Boolean active = true;
}


