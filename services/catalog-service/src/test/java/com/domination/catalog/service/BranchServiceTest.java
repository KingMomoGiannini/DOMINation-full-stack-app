package com.domination.catalog.service;

import com.domination.catalog.domain.Branch;
import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.mapper.BranchMapper;
import com.domination.catalog.repository.BranchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchMapper branchMapper;

    @InjectMocks
    private BranchService branchService;

    @Test
    void getAllActiveBranches_returnsMappedBranches() {
        Branch branch = Branch.builder()
                .id(1L)
                .name("Centro")
                .address("Av. Siempre Viva 123")
                .active(true)
                .providerId(77L)
                .build();
        BranchDTO dto = BranchDTO.builder()
                .id(1L)
                .name("Centro")
                .address("Av. Siempre Viva 123")
                .active(true)
                .providerId(77L)
                .build();

        when(branchRepository.findByActiveTrue()).thenReturn(List.of(branch));
        when(branchMapper.toDTO(branch)).thenReturn(dto);

        List<BranchDTO> result = branchService.getAllActiveBranches();

        assertEquals(1, result.size());
        assertEquals("Centro", result.getFirst().getName());
        verify(branchRepository).findByActiveTrue();
        verify(branchMapper).toDTO(branch);
    }

    @Test
    void getBranchById_throwsWhenMissing() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> branchService.getBranchById(99L));

        verify(branchRepository).findById(99L);
    }
}
