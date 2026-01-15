package com.domination.catalog.service;

import com.domination.catalog.domain.Branch;
import com.domination.catalog.domain.Inventory;
import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentableItem;
import com.domination.catalog.domain.RentalMode;
import com.domination.catalog.dto.CreateItemRequest;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.dto.RoomCreateRequest;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.mapper.ItemMapper;
import com.domination.catalog.repository.BranchRepository;
import com.domination.catalog.repository.InventoryRepository;
import com.domination.catalog.repository.RentableItemRepository;
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
public class ItemService {

    private final RentableItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final ItemMapper itemMapper;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<ItemDTO> getItems(Long branchId, ItemType type) {
        log.debug("Obteniendo items - branchId: {}, type: {}", branchId, type);
        
        List<RentableItem> items;
        if (branchId != null && type != null) {
            items = itemRepository.findByBranchIdAndTypeAndActiveTrue(branchId, type);
        } else if (branchId != null) {
            items = itemRepository.findByBranchIdAndActiveTrue(branchId);
        } else if (type != null) {
            items = itemRepository.findByTypeAndActiveTrue(type);
        } else {
            items = itemRepository.findByActiveTrue();
        }
        
        return items.stream()
                .map(item -> {
                    Integer quantity = inventoryRepository.findByItemId(item.getId())
                            .map(Inventory::getQuantityTotal)
                            .orElse(0);
                    return itemMapper.toDTO(item, quantity);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItemDTO getItemById(Long id) {
        log.debug("Obteniendo item con id: {}", id);
        
        RentableItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item no encontrado con id: " + id));
        
        Integer quantity = inventoryRepository.findByItemId(item.getId())
                .map(Inventory::getQuantityTotal)
                .orElse(0);
        
        return itemMapper.toDTO(item, quantity);
    }

    @Transactional
    public ItemDTO createItem(CreateItemRequest request) {
        log.info("Creando nuevo item: {} en sucursal {}", request.getName(), request.getBranchId());
        
        // Crear el item
        RentableItem item = RentableItem.builder()
                .branchId(request.getBranchId())
                .name(request.getName())
                .type(request.getType())
                .rentalMode(request.getRentalMode())
                .basePrice(request.getBasePrice())
                .active(true)
                .build();
        
        RentableItem savedItem = itemRepository.save(item);
        log.info("Item creado con id: {}", savedItem.getId());
        
        // Crear el inventario
        Inventory inventory = Inventory.builder()
                .branchId(request.getBranchId())
                .itemId(savedItem.getId())
                .quantityTotal(request.getQuantityTotal())
                .build();
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Inventario creado para item {}: cantidad {}", savedItem.getId(), savedInventory.getQuantityTotal());
        
        return itemMapper.toDTO(savedItem, savedInventory.getQuantityTotal());
    }

    @Transactional
    public ItemDTO createRoomForProvider(Long branchId, RoomCreateRequest request, Long providerId) {
        log.info("Provider {} creando sala '{}' en sucursal {}", providerId, request.getName(), branchId);
        
        // Validar que branch existe
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id: " + branchId));
        
        // Validar ownership
        if (!providerId.equals(branch.getProviderId())) {
            log.warn("Provider {} intentó crear sala en sucursal {} que pertenece a provider {}", 
                    providerId, branchId, branch.getProviderId());
            throw new AccessDeniedException("No tienes permiso para crear salas en esta sucursal");
        }
        
        // Crear el item (sala)
        RentableItem room = RentableItem.builder()
                .branchId(branchId)
                .name(request.getName())
                .type(ItemType.ROOM)
                .rentalMode(RentalMode.TIME_EXCLUSIVE)
                .basePrice(request.getHourlyPrice())
                .active(true)
                .build();
        
        RentableItem savedRoom = itemRepository.save(room);
        log.info("Sala creada con id: {} para provider: {}", savedRoom.getId(), providerId);
        
        return itemMapper.toDTO(savedRoom, 0);
    }
}


