package com.domination.booking.repository;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ReservationRepositoryIT {

/*    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");*/

/*    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }*/

    @Autowired
    ReservationRepository reservationRepository;

    @Test
    void findOverlappingReservations_returnsOverlaps_andIgnoresCancelled() {
        Long itemId = 1L;

        LocalDateTime baseStart = LocalDateTime.now().plusDays(1);
        LocalDateTime baseEnd = baseStart.plusHours(2);

        reservationRepository.save(reservationWithLine(itemId, baseStart.minusMinutes(30), baseEnd.minusMinutes(30), ReservationStatus.PENDING, 1));
        reservationRepository.save(reservationWithLine(itemId, baseStart.minusMinutes(10), baseEnd.minusMinutes(10), ReservationStatus.CANCELLED, 1));

        List<Reservation> overlaps = reservationRepository.findOverlappingReservations(
                itemId, baseStart, baseEnd, ReservationStatus.CANCELLED
        );

        assertEquals(1, overlaps.size());
        assertEquals(ReservationStatus.PENDING, overlaps.get(0).getStatus());
    }

    @Test
    void sumReservedQuantity_sumsQuantities_andIgnoresCancelled() {
        Long itemId = 2L;

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        reservationRepository.save(reservationWithLine(itemId, start.minusMinutes(30), end.minusMinutes(30), ReservationStatus.PENDING, 3));
        reservationRepository.save(reservationWithLine(itemId, start.minusMinutes(20), end.minusMinutes(20), ReservationStatus.PENDING, 2));
        reservationRepository.save(reservationWithLine(itemId, start.minusMinutes(10), end.minusMinutes(10), ReservationStatus.CANCELLED, 100));

        Integer sum = reservationRepository.sumReservedQuantity(itemId, start, end, ReservationStatus.CANCELLED);

        assertEquals(5, sum);
    }

    private Reservation reservationWithLine(Long itemId, LocalDateTime start, LocalDateTime end, ReservationStatus status, int qty) {
        Reservation reservation = Reservation.builder()
                .customerId("cust-it")
                .branchId(10L)
                .providerId(777L)
                .startAt(start)
                .endAt(end)
                .status(status)
//                .lines(List.of())
                .build();

        ReservationLine line = ReservationLine.builder()
                .reservation(reservation)
                .itemId(itemId)
                .quantity(qty)
                .price(new BigDecimal("100.00"))
                .build();

        reservation.setLines(List.of(line));
        return reservation;
    }
}
