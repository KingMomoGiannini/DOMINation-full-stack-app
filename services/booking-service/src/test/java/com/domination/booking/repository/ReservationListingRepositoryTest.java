package com.domination.booking.repository;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ProviderReservationMetricsDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking_repo_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.test.database.replace=none",
        "spring.sql.init.mode=never"
})
class ReservationListingRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void fetchProviderReservationMetrics_aggregatesCountsWithSingleScan() {
        LocalDateTime now = LocalDateTime.now();
        reservationRepository.save(reservation("cust-1", 777L, 10L, now.plusDays(1), now.plusDays(1).plusHours(2), ReservationStatus.PENDING));
        reservationRepository.save(reservation("cust-2", 777L, 10L, now.minusHours(3), now.plusHours(1), ReservationStatus.CONFIRMED));
        reservationRepository.save(reservation("cust-3", 777L, 11L, now.minusDays(3), now.minusDays(3).plusHours(2), ReservationStatus.CONFIRMED));
        reservationRepository.save(reservation("cust-4", 777L, 11L, now.plusDays(2), now.plusDays(2).plusHours(1), ReservationStatus.CANCELLED));
        reservationRepository.save(reservation("cust-5", 999L, 99L, now.plusDays(1), now.plusDays(1).plusHours(1), ReservationStatus.PENDING));

        ProviderReservationMetricsDto metrics = reservationRepository.fetchProviderReservationMetrics(777L, now);

        assertEquals(4, metrics.total());
        assertEquals(1, metrics.cancelled());
        assertEquals(1, metrics.upcoming());
        assertEquals(1, metrics.inProgress());
        assertEquals(1, metrics.completed());
    }

    @Test
    void findByCustomerIdOrderByStartAtDescIdDesc_returnsStableDescendingHistory() {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        Reservation older = reservationRepository.save(reservation("cust-10", 777L, 10L, startAt.minusDays(1), startAt.minusDays(1).plusHours(1), ReservationStatus.CONFIRMED));
        Reservation sameStartFirst = reservationRepository.save(reservation("cust-10", 777L, 10L, startAt, startAt.plusHours(1), ReservationStatus.PENDING));
        Reservation sameStartSecond = reservationRepository.save(reservation("cust-10", 777L, 10L, startAt, startAt.plusHours(2), ReservationStatus.CANCELLED));

        List<Reservation> history = reservationRepository.findByCustomerIdOrderByStartAtDescIdDesc("cust-10");

        assertEquals(List.of(sameStartSecond.getId(), sameStartFirst.getId(), older.getId()),
                history.stream().map(Reservation::getId).toList());
    }

    private static Reservation reservation(
            String customerId,
            Long providerId,
            Long branchId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ReservationStatus status) {
        return Reservation.builder()
                .customerId(customerId)
                .providerId(providerId)
                .branchId(branchId)
                .startAt(startAt)
                .endAt(endAt)
                .status(status)
                .build();
    }
}
