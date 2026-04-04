package com.domination.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHold {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @NotNull
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryHoldStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

