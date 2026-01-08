package com.domination.catalog.mapper;

import com.domination.catalog.domain.RentableItem;
import com.domination.catalog.dto.ItemDTO;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public ItemDTO toDTO(RentableItem item, Integer quantityTotal) {
        if (item == null) {
            return null;
        }
        
        return ItemDTO.builder()
                .id(item.getId())
                .branchId(item.getBranchId())
                .name(item.getName())
                .type(item.getType())
                .rentalMode(item.getRentalMode())
                .basePrice(item.getBasePrice())
                .active(item.getActive())
                .quantityTotal(quantityTotal != null ? quantityTotal : 0)
                .build();
    }

    public RentableItem toEntity(ItemDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return RentableItem.builder()
                .id(dto.getId())
                .branchId(dto.getBranchId())
                .name(dto.getName())
                .type(dto.getType())
                .rentalMode(dto.getRentalMode())
                .basePrice(dto.getBasePrice())
                .active(dto.getActive())
                .build();
    }
}


