package com.domination.catalog.repository;

import com.domination.catalog.domain.InventoryHold;
import com.domination.catalog.domain.InventoryHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InventoryHoldRepository extends JpaRepository<InventoryHold, String> {

    @Query("SELECT COALESCE(SUM(h.quantity), 0) FROM InventoryHold h " +
           "WHERE h.itemId = :itemId " +
           "AND h.status = :status " +
           "AND h.startAt < :endAt " +
           "AND h.endAt > :startAt")
    Integer sumHeldQuantityInRange(
            @Param("itemId") Long itemId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("status") InventoryHoldStatus status
    );
}

