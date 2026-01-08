package com.domination.catalog.repository;

import com.domination.catalog.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByBranchIdAndItemId(Long branchId, Long itemId);
    Optional<Inventory> findByItemId(Long itemId);
}


