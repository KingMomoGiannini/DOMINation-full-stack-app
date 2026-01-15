package com.domination.catalog.mapper;

import com.domination.catalog.domain.Branch;
import com.domination.catalog.dto.BranchDTO;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public BranchDTO toDTO(Branch branch) {
        if (branch == null) {
            return null;
        }
        
        return BranchDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .active(branch.getActive())
                .providerId(branch.getProviderId())
                .build();
    }

    public Branch toEntity(BranchDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return Branch.builder()
                .id(dto.getId())
                .name(dto.getName())
                .address(dto.getAddress())
                .active(dto.getActive())
                .providerId(dto.getProviderId())
                .build();
    }
}


