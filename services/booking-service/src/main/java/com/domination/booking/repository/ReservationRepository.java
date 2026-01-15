package com.domination.booking.repository;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByCustomerId(String customerId);
    
    List<Reservation> findByProviderId(Long providerId);
    
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
}


