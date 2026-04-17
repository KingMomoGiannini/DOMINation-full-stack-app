package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ReservationCancellationBlockReason;
import com.domination.booking.dto.ReservationAttendanceStatus;
import com.domination.booking.dto.ReservationOperationalStatus;
import com.domination.booking.dto.ReservationProviderActionBlockReason;
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

    @Test
    void attendanceAndProviderActions_keepTemporalLifecycleSeparatedFromPersistedFacts() {
        LocalDateTime now = LocalDateTime.now();

        Reservation live = reservation(now.minusMinutes(20), now.plusMinutes(40), ReservationStatus.CONFIRMED);
        Reservation checkedIn = reservation(now.minusMinutes(20), now.plusMinutes(40), ReservationStatus.CONFIRMED);
        checkedIn.setCheckedInAt(now.minusMinutes(10));
        Reservation finishedWithoutAttendance = reservation(now.minusHours(2), now.minusHours(1), ReservationStatus.CONFIRMED);
        Reservation noShow = reservation(now.minusHours(2), now.minusHours(1), ReservationStatus.CONFIRMED);
        noShow.setNoShowMarkedAt(now.minusMinutes(30));
        Reservation cancelled = reservation(now.minusHours(2), now.minusHours(1), ReservationStatus.CANCELLED);

        assertEquals(ReservationAttendanceStatus.NOT_RECORDED, resolver.resolveAttendanceStatus(live));
        assertTrue(resolver.isProviderCheckInAllowed(live, now));
        assertEquals(ReservationProviderActionBlockReason.BEFORE_END, resolver.resolveProviderMarkNoShowBlockReason(live, now));

        assertEquals(ReservationAttendanceStatus.CHECKED_IN, resolver.resolveAttendanceStatus(checkedIn));
        assertEquals(ReservationProviderActionBlockReason.ALREADY_CHECKED_IN, resolver.resolveProviderCheckInBlockReason(checkedIn, now));
        assertEquals(ReservationProviderActionBlockReason.ALREADY_CHECKED_IN, resolver.resolveProviderMarkNoShowBlockReason(checkedIn, now));

        assertEquals(ReservationAttendanceStatus.NOT_RECORDED, resolver.resolveAttendanceStatus(finishedWithoutAttendance));
        assertEquals(ReservationProviderActionBlockReason.AFTER_END, resolver.resolveProviderCheckInBlockReason(finishedWithoutAttendance, now));
        assertTrue(resolver.isProviderMarkNoShowAllowed(finishedWithoutAttendance, now));

        assertEquals(ReservationAttendanceStatus.NO_SHOW, resolver.resolveAttendanceStatus(noShow));
        assertEquals(ReservationProviderActionBlockReason.ALREADY_MARKED_NO_SHOW, resolver.resolveProviderMarkNoShowBlockReason(noShow, now));

        assertEquals(ReservationAttendanceStatus.NOT_APPLICABLE, resolver.resolveAttendanceStatus(cancelled));
        assertEquals(ReservationProviderActionBlockReason.CANCELLED, resolver.resolveProviderCheckInBlockReason(cancelled, now));
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
