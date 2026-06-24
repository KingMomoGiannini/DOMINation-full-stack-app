package com.domination.booking.mapper;

import com.domination.booking.domain.ReservationAuditEvent;
import com.domination.booking.dto.ReservationAuditEventDTO;
import org.springframework.stereotype.Component;

@Component
public class ReservationAuditEventMapper {

    public ReservationAuditEventDTO toDTO(ReservationAuditEvent event) {
        if (event == null) {
            return null;
        }
        return ReservationAuditEventDTO.builder()
                .id(event.getId())
                .reservationId(event.getReservation().getId())
                .actorUserId(event.getActorUserId())
                .actorRole(event.getActorRole())
                .eventType(event.getEventType())
                .reason(event.getReason())
                .comment(event.getComment())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
