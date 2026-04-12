package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ReservationCancellationBlockReason;
import com.domination.booking.dto.ReservationOperationalStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationLifecycleResolverTest {

    private final ReservationLifecycleResolver resolver = new ReservationLifecycleResolver();

    @Test
    void resolveOperationalStatus_marksUpcomingInProgressAndCompletedWithoutPersistingThem() {
        LocalDateTime now = LocalDateTime.now();

        assertEquals(
                ReservationOperationalStatus.UPCOMING,
                resolver.resolveOperationalStatus(reservation(now.plusHours(2), now.plusHours(4), ReservationStatus.CONFIRMED), now)
        );
        assertEquals(
                ReservationOperationalStatus.IN_PROGRESS,
                resolver.resolveOperationalStatus(reservation(now.minusMinutes(30), now.plusMinutes(30), ReservationStatus.CONFIRMED), now)
        );
        assertEquals(
                ReservationOperationalStatus.COMPLETED,
                resolver.resolveOperationalStatus(reservation(now.minusHours(4), now.minusHours(1), ReservationStatus.CONFIRMED), now)
        );
        assertEquals(
                ReservationOperationalStatus.CANCELLED,
                resolver.resolveOperationalStatus(reservation(now.plusHours(1), now.plusHours(2), ReservationStatus.CANCELLED), now)
        );
    }

    @Test
    void cancellationFlags_followCurrentRule_beforeStartOnly() {
        LocalDateTime now = LocalDateTime.now();

        Reservation upcoming = reservation(now.plusHours(2), now.plusHours(4), ReservationStatus.CONFIRMED);
        Reservation live = reservation(now.minusMinutes(15), now.plusMinutes(45), ReservationStatus.CONFIRMED);
        Reservation cancelled = reservation(now.plusHours(2), now.plusHours(4), ReservationStatus.CANCELLED);

        assertTrue(resolver.isCancellable(upcoming, now));
        assertNull(resolver.resolveCancellationBlockReason(upcoming, now));

        assertFalse(resolver.isCancellable(live, now));
        assertEquals(ReservationCancellationBlockReason.ALREADY_STARTED, resolver.resolveCancellationBlockReason(live, now));

        assertFalse(resolver.isCancellable(cancelled, now));
        assertEquals(ReservationCancellationBlockReason.ALREADY_CANCELLED, resolver.resolveCancellationBlockReason(cancelled, now));
    }

    private static Reservation reservation(LocalDateTime startAt, LocalDateTime endAt, ReservationStatus status) {
        return Reservation.builder()
                .customerId("cust-1")
                .branchId(10L)
                .providerId(777L)
                .startAt(startAt)
                .endAt(endAt)
                .status(status)
                .build();
    }
}
