package com.domination.booking.repository;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.ProviderReservationMetricsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    List<Reservation> findByCustomerIdOrderByStartAtDescIdDesc(String customerId);

    List<Reservation> findByProviderId(Long providerId);

    @Query("""
            select new com.domination.booking.dto.ProviderReservationMetricsDto(
                count(r),
                coalesce(sum(case when r.status = com.domination.booking.domain.ReservationStatus.CANCELLED then 1 else 0 end), 0),
                coalesce(sum(case when r.status <> com.domination.booking.domain.ReservationStatus.CANCELLED and r.startAt > :now then 1 else 0 end), 0),
                coalesce(sum(case when r.status <> com.domination.booking.domain.ReservationStatus.CANCELLED and r.startAt <= :now and r.endAt > :now then 1 else 0 end), 0),
                coalesce(sum(case when r.status <> com.domination.booking.domain.ReservationStatus.CANCELLED and r.endAt <= :now then 1 else 0 end), 0)
            )
            from Reservation r
            where r.providerId = :providerId
            """)
    ProviderReservationMetricsDto fetchProviderReservationMetrics(
            @Param("providerId") Long providerId,
            @Param("now") LocalDateTime now
    );
    
    /**
     * Encuentra reservas que se solapan con el rango de tiempo dado para un item específico
     * Excluye reservas canceladas
     */
    @Query("SELECT r FROM Reservation r " +
           "JOIN r.lines l " +
           "WHERE l.itemId = :itemId " +
           "AND r.status != :excludeStatus " +
           "AND r.startAt < :endAt " +
           "AND r.endAt > :startAt")
    List<Reservation> findOverlappingReservations(
            @Param("itemId") Long itemId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeStatus") ReservationStatus excludeStatus
    );
    
    /**
     * Suma las cantidades reservadas de un item en un rango de tiempo
     * Excluye reservas canceladas
     */
    @Query("SELECT COALESCE(SUM(l.quantity), 0) FROM Reservation r " +
           "JOIN r.lines l " +
           "WHERE l.itemId = :itemId " +
           "AND r.status != :excludeStatus " +
           "AND r.startAt < :endAt " +
           "AND r.endAt > :startAt")
    Integer sumReservedQuantity(
            @Param("itemId") Long itemId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeStatus") ReservationStatus excludeStatus
    );

    Optional<Reservation> findByIdAndCustomerId(Long id, String customerId);

}


