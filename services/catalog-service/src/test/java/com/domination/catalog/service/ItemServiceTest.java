package com.domination.catalog.service;

import com.domination.catalog.domain.Inventory;
import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentableItem;
import com.domination.catalog.domain.RentalMode;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.mapper.ItemMapper;
import com.domination.catalog.repository.BranchRepository;
import com.domination.catalog.repository.InventoryRepository;
import com.domination.catalog.repository.RentableItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private RentableItemRepository itemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItems_filtersByBranchAndType_andMapsInventoryQuantity() {
        RentableItem item = RentableItem.builder()
                .id(5L)
                .branchId(2L)
                .name("Sala A")
                .type(ItemType.ROOM)
                .rentalMode(RentalMode.TIME_EXCLUSIVE)
                .basePrice(new BigDecimal("12000"))
                .active(true)
                .build();
        ItemDTO dto = ItemDTO.builder()
                .id(5L)
                .branchId(2L)
                .name("Sala A")
                .type(ItemType.ROOM)
                .rentalMode(RentalMode.TIME_EXCLUSIVE)
                .basePrice(new BigDecimal("12000"))
                .active(true)
                .quantityTotal(0)
                .build();

        when(itemRepository.findByBranchIdAndTypeAndActiveTrue(2L, ItemType.ROOM)).thenReturn(List.of(item));
        when(inventoryRepository.findByItemId(5L)).thenReturn(Optional.empty());
        when(itemMapper.toDTO(item, 0)).thenReturn(dto);

        List<ItemDTO> result = itemService.getItems(2L, ItemType.ROOM);

        assertEquals(1, result.size());
        assertEquals("Sala A", result.getFirst().getName());
        verify(itemRepository).findByBranchIdAndTypeAndActiveTrue(2L, ItemType.ROOM);
        verify(itemMapper).toDTO(item, 0);
    }

    @Test
    void getItemById_throwsWhenMissing() {
        when(itemRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itemService.getItemById(404L));

        verify(itemRepository).findById(404L);
    }

    @Test
    void getItemById_usesInventoryQuantityWhenPresent() {
        RentableItem item = RentableItem.builder()
                .id(9L)
                .branchId(3L)
                .name("Bateria")
                .type(ItemType.INSTRUMENT)
                .rentalMode(RentalMode.TIME_QUANTITY)
                .basePrice(new BigDecimal("3000"))
                .active(true)
                .build();
        ItemDTO dto = ItemDTO.builder()
                .id(9L)
                .name("Bateria")
                .quantityTotal(4)
                .build();
        Inventory inventory = Inventory.builder()
                .id(1L)
                .branchId(3L)
                .itemId(9L)
                .quantityTotal(4)
                .build();

        when(itemRepository.findById(9L)).thenReturn(Optional.of(item));
        when(inventoryRepository.findByItemId(9L)).thenReturn(Optional.of(inventory));
        when(itemMapper.toDTO(item, 4)).thenReturn(dto);

        ItemDTO result = itemService.getItemById(9L);

        assertEquals(4, result.getQuantityTotal());
        verify(itemMapper).toDTO(item, 4);
    }
}
