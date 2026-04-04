package com.domination.booking.model;

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
    private Long itemId;
    private Integer quantity;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String reference;
}

