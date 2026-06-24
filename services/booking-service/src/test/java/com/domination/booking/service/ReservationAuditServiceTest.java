package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationAuditEvent;
import com.domination.booking.domain.ReservationAuditEventType;
import com.domination.booking.dto.ReservationAuditEventDTO;
import com.domination.booking.exception.NotFoundException;
import com.domination.booking.mapper.ReservationAuditEventMapper;
import com.domination.booking.repository.ReservationAuditEventRepository;
import com.domination.booking.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationAuditServiceTest {

    @Mock private ReservationAuditEventRepository auditEventRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationAuditEventMapper auditEventMapper;

    @InjectMocks private ReservationAuditService reservationAuditService;

    @Test
    void recordCreated_savesCreatedEventWithCustomerActor() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .customerId("101")
                .build();

        ArgumentCaptor<ReservationAuditEvent> captor = ArgumentCaptor.forClass(ReservationAuditEvent.class);

        reservationAuditService.recordCreated(reservation, "101");

        verify(auditEventRepository).save(captor.capture());
        ReservationAuditEvent saved = captor.getValue();
        assertEquals(reservation, saved.getReservation());
        assertEquals("101", saved.getActorUserId());
        assertEquals("ROLE_USER", saved.getActorRole());
        assertEquals(ReservationAuditEventType.CREATED, saved.getEventType());
        assertEquals("RESERVATION_CREATED", saved.getReason());
    }

    @Test
    void getAuditForReservation_allowsCustomerOwner() {
        Reservation reservation = Reservation.builder().id(10L).customerId("101").build();
        ReservationAuditEvent event = auditEvent(reservation, ReservationAuditEventType.CREATED);
        ReservationAuditEventDTO dto = ReservationAuditEventDTO.builder().id(1L).reservationId(10L).build();

        when(reservationRepository.findByIdAndCustomerId(10L, "101")).thenReturn(Optional.of(reservation));
        when(auditEventRepository.findByReservationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(event));
        when(auditEventMapper.toDTO(event)).thenReturn(dto);

        List<ReservationAuditEventDTO> result = reservationAuditService.getAuditForReservation(
                10L,
                "101",
                List.of("ROLE_USER")
        );

        assertEquals(List.of(dto), result);
        verify(auditEventRepository).findByReservationIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void getAuditForReservation_allowsProviderOwner() {
        Reservation reservation = Reservation.builder().id(10L).providerId(777L).build();
        ReservationAuditEvent event = auditEvent(reservation, ReservationAuditEventType.CHECKED_IN);
        ReservationAuditEventDTO dto = ReservationAuditEventDTO.builder().id(2L).reservationId(10L).build();

        when(reservationRepository.findByIdAndProviderId(10L, 777L)).thenReturn(Optional.of(reservation));
        when(auditEventRepository.findByReservationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(event));
        when(auditEventMapper.toDTO(event)).thenReturn(dto);

        List<ReservationAuditEventDTO> result = reservationAuditService.getAuditForReservation(
                10L,
                "777",
                List.of("ROLE_PROVIDER")
        );

        assertEquals(List.of(dto), result);
        verify(auditEventRepository).findByReservationIdOrderByCreatedAtAsc(10L);
    }

    @Test
    void getAuditForReservation_allowsAdminWithoutOwnershipLookup() {
        Reservation reservation = Reservation.builder().id(10L).build();
        ReservationAuditEvent event = auditEvent(reservation, ReservationAuditEventType.MARKED_NO_SHOW);
        ReservationAuditEventDTO dto = ReservationAuditEventDTO.builder().id(3L).reservationId(10L).build();

        when(auditEventRepository.findByReservationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(event));
        when(auditEventMapper.toDTO(event)).thenReturn(dto);

        List<ReservationAuditEventDTO> result = reservationAuditService.getAuditForReservation(
                10L,
                "1",
                List.of("ROLE_ADMIN")
        );

        assertEquals(List.of(dto), result);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getAuditForReservation_rejectsUserWithoutOwnership() {
        when(reservationRepository.findByIdAndCustomerId(10L, "202")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reservationAuditService.getAuditForReservation(
                10L,
                "202",
                List.of("ROLE_USER")
        ));

        verifyNoInteractions(auditEventRepository);
    }

    private static ReservationAuditEvent auditEvent(Reservation reservation, ReservationAuditEventType eventType) {
        return ReservationAuditEvent.builder()
                .id(1L)
                .reservation(reservation)
                .actorUserId("101")
                .actorRole("ROLE_USER")
                .eventType(eventType)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
