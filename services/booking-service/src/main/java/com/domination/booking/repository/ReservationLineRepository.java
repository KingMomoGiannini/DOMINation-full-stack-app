package com.domination.booking.repository;

import com.domination.booking.domain.ReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationLineRepository extends JpaRepository<ReservationLine, Long> {
}


