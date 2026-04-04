package com.domination.catalog.dto;

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
public class HoldInventoryResponse {
    private String holdId;
    private LocalDateTime expiresAt;
    private Long itemId;
    private Integer quantity;
}

