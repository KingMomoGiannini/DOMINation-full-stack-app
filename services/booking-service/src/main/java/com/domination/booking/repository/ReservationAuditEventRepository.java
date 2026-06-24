package com.domination.booking.repository;

import com.domination.booking.domain.ReservationAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationAuditEventRepository extends JpaRepository<ReservationAuditEvent, Long> {

    List<ReservationAuditEvent> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
