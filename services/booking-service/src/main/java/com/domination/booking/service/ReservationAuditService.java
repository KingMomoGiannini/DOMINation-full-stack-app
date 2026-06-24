package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationAuditEvent;
import com.domination.booking.domain.ReservationAuditEventType;
import com.domination.booking.dto.ReservationAuditEventDTO;
import com.domination.booking.exception.NotFoundException;
import com.domination.booking.mapper.ReservationAuditEventMapper;
import com.domination.booking.repository.ReservationAuditEventRepository;
import com.domination.booking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationAuditService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_PROVIDER = "ROLE_PROVIDER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ReservationAuditEventRepository auditEventRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationAuditEventMapper auditEventMapper;

    @Transactional
    public void recordCreated(Reservation reservation, String customerId) {
        recordEvent(
                reservation,
                customerId,
                ROLE_USER,
                ReservationAuditEventType.CREATED,
                "RESERVATION_CREATED",
                null
        );
    }

    @Transactional
    public void recordCancelledByCustomer(Reservation reservation, String customerId) {
        recordEvent(
                reservation,
                customerId,
                ROLE_USER,
                ReservationAuditEventType.CANCELLED_BY_CUSTOMER,
                "CUSTOMER_REQUEST",
                null
        );
    }

    @Transactional
    public void recordCheckedIn(Reservation reservation, Long providerId) {
        recordEvent(
                reservation,
                String.valueOf(providerId),
                ROLE_PROVIDER,
                ReservationAuditEventType.CHECKED_IN,
                "PROVIDER_CONFIRMED_ATTENDANCE",
                null
        );
    }

    @Transactional
    public void recordMarkedNoShow(Reservation reservation, Long providerId) {
        recordEvent(
                reservation,
                String.valueOf(providerId),
                ROLE_PROVIDER,
                ReservationAuditEventType.MARKED_NO_SHOW,
                "CUSTOMER_DID_NOT_ATTEND",
                null
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationAuditEventDTO> getAuditForReservation(
            Long reservationId,
            String actorUserId,
            Collection<String> actorRoles) {
        if (!canViewReservationAudit(reservationId, actorUserId, actorRoles)) {
            throw new NotFoundException("Reserva no encontrada");
        }
        return auditEventRepository.findByReservationIdOrderByCreatedAtAsc(reservationId).stream()
                .map(auditEventMapper::toDTO)
                .toList();
    }

    private void recordEvent(
            Reservation reservation,
            String actorUserId,
            String actorRole,
            ReservationAuditEventType eventType,
            String reason,
            String comment) {
        ReservationAuditEvent event = ReservationAuditEvent.builder()
                .reservation(reservation)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .eventType(eventType)
                .reason(reason)
                .comment(comment)
                .build();
        auditEventRepository.save(event);
    }

    private boolean canViewReservationAudit(Long reservationId, String actorUserId, Collection<String> actorRoles) {
        if (hasRole(actorRoles, ROLE_ADMIN)) {
            return true;
        }
        if (hasRole(actorRoles, ROLE_USER)
                && reservationRepository.findByIdAndCustomerId(reservationId, actorUserId).isPresent()) {
            return true;
        }
        if (hasRole(actorRoles, ROLE_PROVIDER)) {
            return parseProviderId(actorUserId)
                    .map(providerId -> reservationRepository.findByIdAndProviderId(reservationId, providerId).isPresent())
                    .orElse(false);
        }
        return false;
    }

    private static boolean hasRole(Collection<String> actorRoles, String expectedRole) {
        if (actorRoles == null || actorRoles.isEmpty()) {
            return false;
        }
        return actorRoles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .anyMatch(expectedRole::equals);
    }

    private static Optional<Long> parseProviderId(String actorUserId) {
        try {
            return Optional.of(Long.parseLong(actorUserId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
