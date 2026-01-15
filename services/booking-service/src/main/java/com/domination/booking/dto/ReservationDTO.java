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
    private Long branchId;
    private Long providerId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private List<ReservationLineDTO> lines = new ArrayList<>();
}


