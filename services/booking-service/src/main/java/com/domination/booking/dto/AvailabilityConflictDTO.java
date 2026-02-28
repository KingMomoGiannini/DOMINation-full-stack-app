package com.domination.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityConflictDTO {
    private Long itemId;
    private String reason;
    private String detail;
    private Integer requestedQty;
    private Integer availableQty;
    private Integer reservedQty;
}
