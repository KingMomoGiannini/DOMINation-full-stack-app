package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ReservationCancellationBlockReason;
import com.domination.booking.dto.ReservationOperationalStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReservationLifecycleResolver {

    public ReservationOperationalStatus resolveOperationalStatus(Reservation reservation, LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationOperationalStatus.CANCELLED;
        }
        if (now.isBefore(reservation.getStartAt())) {
            return ReservationOperationalStatus.UPCOMING;
        }
        if (now.isBefore(reservation.getEndAt())) {
            return ReservationOperationalStatus.IN_PROGRESS;
        }
        return ReservationOperationalStatus.COMPLETED;
    }

    public boolean isCancellable(Reservation reservation, LocalDateTime now) {
        return reservation.getStatus() != ReservationStatus.CANCELLED && now.isBefore(reservation.getStartAt());
    }

    public ReservationCancellationBlockReason resolveCancellationBlockReason(
            Reservation reservation,
            LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationCancellationBlockReason.ALREADY_CANCELLED;
        }
        if (!now.isBefore(reservation.getStartAt())) {
            return ReservationCancellationBlockReason.ALREADY_STARTED;
        }
        return null;
    }
}
