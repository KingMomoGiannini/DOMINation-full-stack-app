package com.domination.catalog.repository;

import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentableItemRepository extends JpaRepository<RentableItem, Long> {
    List<RentableItem> findByActiveTrue();
    List<RentableItem> findByBranchIdAndActiveTrue(Long branchId);
    List<RentableItem> findByTypeAndActiveTrue(ItemType type);
    List<RentableItem> findByBranchIdAndTypeAndActiveTrue(Long branchId, ItemType type);
}


