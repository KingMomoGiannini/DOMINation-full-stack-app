package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ReservationCancellationBlockReason;
import com.domination.booking.dto.ReservationAttendanceStatus;
import com.domination.booking.dto.ReservationOperationalStatus;
import com.domination.booking.dto.ReservationProviderActionBlockReason;
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

    public ReservationAttendanceStatus resolveAttendanceStatus(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationAttendanceStatus.NOT_APPLICABLE;
        }
        if (reservation.getCheckedInAt() != null) {
            return ReservationAttendanceStatus.CHECKED_IN;
        }
        if (reservation.getNoShowMarkedAt() != null) {
            return ReservationAttendanceStatus.NO_SHOW;
        }
        return ReservationAttendanceStatus.NOT_RECORDED;
    }

    public boolean isProviderCheckInAllowed(Reservation reservation, LocalDateTime now) {
        return resolveProviderCheckInBlockReason(reservation, now) == null;
    }

    public ReservationProviderActionBlockReason resolveProviderCheckInBlockReason(
            Reservation reservation,
            LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationProviderActionBlockReason.CANCELLED;
        }
        if (reservation.getCheckedInAt() != null) {
            return ReservationProviderActionBlockReason.ALREADY_CHECKED_IN;
        }
        if (reservation.getNoShowMarkedAt() != null) {
            return ReservationProviderActionBlockReason.ALREADY_MARKED_NO_SHOW;
        }
        if (now.isBefore(reservation.getStartAt())) {
            return ReservationProviderActionBlockReason.BEFORE_START;
        }
        if (!now.isBefore(reservation.getEndAt())) {
            return ReservationProviderActionBlockReason.AFTER_END;
        }
        return null;
    }

    public boolean isProviderMarkNoShowAllowed(Reservation reservation, LocalDateTime now) {
        return resolveProviderMarkNoShowBlockReason(reservation, now) == null;
    }

    public ReservationProviderActionBlockReason resolveProviderMarkNoShowBlockReason(
            Reservation reservation,
            LocalDateTime now) {
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationProviderActionBlockReason.CANCELLED;
        }
        if (reservation.getCheckedInAt() != null) {
            return ReservationProviderActionBlockReason.ALREADY_CHECKED_IN;
        }
        if (reservation.getNoShowMarkedAt() != null) {
            return ReservationProviderActionBlockReason.ALREADY_MARKED_NO_SHOW;
        }
        if (now.isBefore(reservation.getEndAt())) {
            return ReservationProviderActionBlockReason.BEFORE_END;
        }
        return null;
    }
}
