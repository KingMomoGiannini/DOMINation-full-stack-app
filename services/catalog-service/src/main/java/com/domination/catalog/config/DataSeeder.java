package com.domination.catalog.config;

import com.domination.catalog.domain.*;
import com.domination.catalog.repository.BranchRepository;
import com.domination.catalog.repository.InventoryRepository;
import com.domination.catalog.repository.RentableItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeder para datos iniciales de prueba
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final RentableItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (branchRepository.count() > 0) {
            log.info("Datos ya existentes. Skipping seed.");
            return;
        }

        log.info("Iniciando seed de datos...");

        // Crear sucursales
        Branch branch1 = Branch.builder()
                .name("DOMINation Buenos Aires Centro")
                .address("Av. Corrientes 1234, CABA")
                .active(true)
                .build();
        branch1 = branchRepository.save(branch1);

        Branch branch2 = Branch.builder()
                .name("DOMINation Belgrano")
                .address("Av. Cabildo 5678, CABA")
                .active(true)
                .build();
        branch2 = branchRepository.save(branch2);

        log.info("Sucursales creadas: {} y {}", branch1.getName(), branch2.getName());

        // Crear items para branch1
        RentableItem sala1 = RentableItem.builder()
                .branchId(branch1.getId())
                .name("Sala A - Grande con batería")
                .type(ItemType.ROOM)
                .rentalMode(RentalMode.TIME_EXCLUSIVE)
                .basePrice(new BigDecimal("1500.00"))
                .active(true)
                .build();
        sala1 = itemRepository.save(sala1);

        Inventory inv1 = Inventory.builder()
                .branchId(branch1.getId())
                .itemId(sala1.getId())
                .quantityTotal(1)
                .build();
        inventoryRepository.save(inv1);

        RentableItem sala2 = RentableItem.builder()
                .branchId(branch1.getId())
                .name("Sala B - Mediana acústica")
                .type(ItemType.ROOM)
                .rentalMode(RentalMode.TIME_EXCLUSIVE)
                .basePrice(new BigDecimal("1200.00"))
                .active(true)
                .build();
        sala2 = itemRepository.save(sala2);

        Inventory inv2 = Inventory.builder()
                .branchId(branch1.getId())
                .itemId(sala2.getId())
                .quantityTotal(1)
                .build();
        inventoryRepository.save(inv2);

        // Crear items para branch2
        RentableItem guitarra = RentableItem.builder()
                .branchId(branch2.getId())
                .name("Guitarra Eléctrica Fender")
                .type(ItemType.INSTRUMENT)
                .rentalMode(RentalMode.TIME_QUANTITY)
                .basePrice(new BigDecimal("500.00"))
                .active(true)
                .build();
        guitarra = itemRepository.save(guitarra);

        Inventory inv3 = Inventory.builder()
                .branchId(branch2.getId())
                .itemId(guitarra.getId())
                .quantityTotal(5)
                .build();
        inventoryRepository.save(inv3);

        RentableItem bajo = RentableItem.builder()
                .branchId(branch2.getId())
                .name("Bajo Eléctrico Ibanez")
                .type(ItemType.INSTRUMENT)
                .rentalMode(RentalMode.TIME_QUANTITY)
                .basePrice(new BigDecimal("450.00"))
                .active(true)
                .build();
        bajo = itemRepository.save(bajo);

        Inventory inv4 = Inventory.builder()
                .branchId(branch2.getId())
                .itemId(bajo.getId())
                .quantityTotal(3)
                .build();
        inventoryRepository.save(inv4);

        log.info("Seed completado: 2 sucursales, 4 items con inventario");
    }
}


