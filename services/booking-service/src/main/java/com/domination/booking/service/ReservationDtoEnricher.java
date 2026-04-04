package com.domination.booking.service;

import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Completa DTOs con datos legibles: catálogo para snapshots faltantes (legacy) y auth para el contexto provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationDtoEnricher {

    private final CatalogClient catalogClient;
    private final AuthClient authClient;

    public void enrichMissingCatalogFields(List<ReservationDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (ReservationDTO dto : dtos) {
            if (dto.getBranchName() == null || dto.getBranchName().isBlank()) {
                try {
                    dto.setBranchName(catalogClient.getBranchDetail(dto.getBranchId()).getName());
                } catch (Exception e) {
                    log.debug("No se pudo rellenar branchName para branchId={}: {}", dto.getBranchId(), e.getMessage());
                }
            }
            if (dto.getLines() != null) {
                for (ReservationLineDTO line : dto.getLines()) {
                    if (line.getItemName() == null || line.getItemName().isBlank()) {
                        try {
                            line.setItemName(catalogClient.getItemDetail(line.getItemId()).getName());
                        } catch (Exception e) {
                            log.debug("No se pudo rellenar itemName para itemId={}: {}", line.getItemId(), e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void enrichCustomerUsernamesForProvider(List<ReservationDTO> dtos, String authorizationHeader) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        Map<Long, String> cache = new HashMap<>();
        for (ReservationDTO dto : dtos) {
            Long uid = parseCustomerId(dto.getCustomerId());
            if (uid == null) {
                continue;
            }
            String username = cache.computeIfAbsent(uid, id ->
                    authClient.getUsernameForProvider(id, authorizationHeader).orElse(null));
            if (username != null) {
                dto.setCustomerUsername(username);
            }
        }
    }

    private Long parseCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(customerId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
