package com.domination.booking.dto;

import com.domination.booking.domain.ReservationAuditEventType;
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
public class ReservationAuditEventDTO {

    private Long id;
    private Long reservationId;
    private String actorUserId;
    private String actorRole;
    private ReservationAuditEventType eventType;
    private String reason;
    private String comment;
    private LocalDateTime createdAt;
}
