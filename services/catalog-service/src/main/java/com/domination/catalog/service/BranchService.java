package com.domination.catalog.service;

import com.domination.catalog.domain.Branch;
import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.mapper.BranchMapper;
import com.domination.catalog.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Transactional(readOnly = true)
    public List<BranchDTO> getAllActiveBranches() {
        log.debug("Obteniendo todas las sucursales activas");
        return branchRepository.findByActiveTrue().stream()
                .map(branchMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BranchDTO getBranchById(Long id) {
        log.debug("Obteniendo sucursal con id: {}", id);
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + id));
        return branchMapper.toDTO(branch);
    }

    @Transactional
    public BranchDTO createBranch(CreateBranchRequest request) {
        log.info("Creando nueva sucursal: {}", request.getName());
        
        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .active(true)
                .build();
        
        Branch saved = branchRepository.save(branch);
        log.info("Sucursal creada con id: {}", saved.getId());
        
        return branchMapper.toDTO(saved);
    }
}


