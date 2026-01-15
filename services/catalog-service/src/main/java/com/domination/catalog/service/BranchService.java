package com.domination.catalog.service;

import com.domination.catalog.domain.Branch;
import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.dto.CreateBranchRequest;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.mapper.BranchMapper;
import com.domination.catalog.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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

    // ============ MÉTODOS PARA PROVIDERS ============

    @Transactional(readOnly = true)
    public List<BranchDTO> findByProviderId(Long providerId) {
        log.debug("Obteniendo sucursales del provider: {}", providerId);
        return branchRepository.findByProviderId(providerId).stream()
                .map(branchMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BranchDTO createForProvider(CreateBranchRequest request, Long providerId) {
        log.info("Provider {} creando sucursal: {}", providerId, request.getName());
        
        Branch branch = Branch.builder()
                .name(request.getName())
                .address(request.getAddress())
                .active(true)
                .providerId(providerId)  // Auto-asignar provider desde JWT
                .build();
        
        Branch saved = branchRepository.save(branch);
        log.info("Sucursal creada con id: {} para provider: {}", saved.getId(), providerId);
        
        return branchMapper.toDTO(saved);
    }

    @Transactional
    public BranchDTO updateForProvider(Long id, CreateBranchRequest request, Long providerId) {
        log.info("Provider {} actualizando sucursal: {}", providerId, id);
        
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + id));
        
        // VALIDAR OWNERSHIP
        if (!providerId.equals(branch.getProviderId())) {
            log.warn("Provider {} intentó editar sucursal {} que pertenece a provider {}", 
                    providerId, id, branch.getProviderId());
            throw new AccessDeniedException("No tienes permiso para editar esta sucursal");
        }
        
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        
        Branch updated = branchRepository.save(branch);
        log.info("Sucursal {} actualizada por provider: {}", id, providerId);
        
        return branchMapper.toDTO(updated);
    }

    @Transactional
    public void deleteForProvider(Long id, Long providerId) {
        log.info("Provider {} eliminando sucursal: {}", providerId, id);
        
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + id));
        
        // VALIDAR OWNERSHIP
        if (!providerId.equals(branch.getProviderId())) {
            log.warn("Provider {} intentó eliminar sucursal {} que pertenece a provider {}", 
                    providerId, id, branch.getProviderId());
            throw new AccessDeniedException("No tienes permiso para eliminar esta sucursal");
        }
        
        branchRepository.deleteById(id);
        log.info("Sucursal {} eliminada por provider: {}", id, providerId);
    }

    @Transactional
    public BranchDTO setActiveForProvider(Long id, Boolean active, Long providerId) {
        log.info("Provider {} {} sucursal: {}", providerId, active ? "habilitando" : "inhabilitando", id);
        
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + id));
        
        // VALIDAR OWNERSHIP
        if (!providerId.equals(branch.getProviderId())) {
            log.warn("Provider {} intentó modificar estado de sucursal {} que pertenece a provider {}", 
                    providerId, id, branch.getProviderId());
            throw new AccessDeniedException("No tienes permiso para modificar esta sucursal");
        }
        
        branch.setActive(active);
        Branch updated = branchRepository.save(branch);
        log.info("Sucursal {} {} por provider: {}", id, active ? "habilitada" : "inhabilitada", providerId);
        
        return branchMapper.toDTO(updated);
    }
}


