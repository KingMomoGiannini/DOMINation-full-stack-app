package com.domination.catalog.service;

import com.domination.catalog.domain.Inventory;
import com.domination.catalog.domain.InventoryHold;
import com.domination.catalog.domain.InventoryHoldStatus;
import com.domination.catalog.dto.HoldInventoryRequest;
import com.domination.catalog.dto.HoldInventoryResponse;
import com.domination.catalog.dto.ReleaseInventoryRequest;
import com.domination.catalog.dto.ReleaseInventoryResponse;
import com.domination.catalog.exception.InventoryHoldConflictException;
import com.domination.catalog.repository.InventoryHoldRepository;
import com.domination.catalog.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryHoldServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryHoldRepository inventoryHoldRepository;

    @InjectMocks private InventoryHoldService inventoryHoldService;

    @Test
    void holdInventory_throwsConflict_whenRequestedExceedsAvailableInRange() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        HoldInventoryRequest request = HoldInventoryRequest.builder()
                .itemId(10L)
                .quantity(5)
                .startAt(start)
                .endAt(end)
                .reference("booking-1")
                .build();

        Inventory inventory = Inventory.builder()
                .id(1L)
                .branchId(2L)
                .itemId(10L)
                .quantityTotal(10)
                .build();

        when(inventoryRepository.findByItemId(10L)).thenReturn(Optional.of(inventory));
        when(inventoryHoldRepository.sumHeldQuantityInRange(
                eq(10L), eq(start), eq(end), eq(InventoryHoldStatus.HELD)
        )).thenReturn(8);

        assertThrows(InventoryHoldConflictException.class,
                () -> inventoryHoldService.holdInventory(request));

        verify(inventoryHoldRepository, never()).save(any(InventoryHold.class));
    }

    @Test
    void holdInventory_createsHold_whenStockIsAvailable() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        HoldInventoryRequest request = HoldInventoryRequest.builder()
                .itemId(11L)
                .quantity(3)
                .startAt(start)
                .endAt(end)
                .reference("booking-2")
                .build();

        Inventory inventory = Inventory.builder()
                .id(1L)
                .branchId(2L)
                .itemId(11L)
                .quantityTotal(10)
                .build();

        when(inventoryRepository.findByItemId(11L)).thenReturn(Optional.of(inventory));
        when(inventoryHoldRepository.sumHeldQuantityInRange(
                eq(11L), eq(start), eq(end), eq(InventoryHoldStatus.HELD)
        )).thenReturn(4);

        ArgumentCaptor<InventoryHold> captor = ArgumentCaptor.forClass(InventoryHold.class);
        when(inventoryHoldRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        HoldInventoryResponse response = inventoryHoldService.holdInventory(request);

        assertNotNull(response.getHoldId());
        assertEquals(11L, response.getItemId());
        assertEquals(3, response.getQuantity());
        assertNull(response.getExpiresAt());

        InventoryHold saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals(11L, saved.getItemId());
        assertEquals(3, saved.getQuantity());
        assertEquals(start, saved.getStartAt());
        assertEquals(end, saved.getEndAt());
        assertEquals(InventoryHoldStatus.HELD, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void releaseInventory_marksReleased_whenHoldExistsAndIsHeld() {
        InventoryHold hold = InventoryHold.builder()
                .id("hold-1")
                .itemId(10L)
                .quantity(2)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(1))
                .status(InventoryHoldStatus.HELD)
                .createdAt(LocalDateTime.now())
                .build();

        when(inventoryHoldRepository.findById("hold-1")).thenReturn(Optional.of(hold));

        ReleaseInventoryResponse response = inventoryHoldService.releaseInventory(
                ReleaseInventoryRequest.builder().holdId("hold-1").build()
        );

        assertTrue(response.isReleased());
        assertEquals(InventoryHoldStatus.RELEASED, hold.getStatus());
        verify(inventoryHoldRepository).save(hold);
    }

    @Test
    void releaseInventory_returnsFalse_whenHoldDoesNotExist() {
        when(inventoryHoldRepository.findById("missing")).thenReturn(Optional.empty());

        ReleaseInventoryResponse response = inventoryHoldService.releaseInventory(
                ReleaseInventoryRequest.builder().holdId("missing").build()
        );

        assertFalse(response.isReleased());
        verify(inventoryHoldRepository, never()).save(any(InventoryHold.class));
    }
}

