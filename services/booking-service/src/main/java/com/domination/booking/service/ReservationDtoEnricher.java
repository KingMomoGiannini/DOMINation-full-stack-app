package com.domination.booking.service;

import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Completa DTOs con datos legibles: catálogo para snapshots faltantes (legacy) y auth para el contexto provider.
 * Fallos de dependencias no abortan el listado; se degrada sin nombre/username.
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
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        Map<Long, Optional<String>> branchCache = new HashMap<>();
        Map<Long, Optional<String>> itemCache = new HashMap<>();

        for (ReservationDTO dto : dtos) {
            if (dto.getBranchName() == null || dto.getBranchName().isBlank()) {
                branchCache
                        .computeIfAbsent(dto.getBranchId(), catalogClient::fetchBranchNameForEnrichment)
                        .ifPresent(dto::setBranchName);
            }
            if (dto.getLines() != null) {
                for (ReservationLineDTO line : dto.getLines()) {
                    if (line.getItemName() == null || line.getItemName().isBlank()) {
                        itemCache
                                .computeIfAbsent(line.getItemId(), catalogClient::fetchItemNameForEnrichment)
                                .ifPresent(line::setItemName);
                    }
                }
            }
        }
        log.debug("reservation enrichment: catalog pass completado reservas={} [requestId={}]", dtos.size(), rid);
    }

    public void enrichCustomerUsernamesForProvider(List<ReservationDTO> dtos, String authorizationHeader) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.debug("reservation enrichment: auth username omitido (sin Authorization) [requestId={}]",
                    Optional.ofNullable(MDC.get("requestId")).orElse("-"));
            return;
        }
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        Map<Long, String> cache = new HashMap<>();
        for (ReservationDTO dto : dtos) {
            Long uid = parseCustomerId(dto.getCustomerId());
            if (uid == null) {
                log.debug("reservation enrichment: customerId no numérico, se omite auth userId lookup id={} [requestId={}]",
                        dto.getCustomerId(), rid);
                continue;
            }
            String username = cache.computeIfAbsent(uid,
                    id -> authClient.getUsernameForProvider(id, authorizationHeader).orElse(null));
            if (username != null) {
                dto.setCustomerUsername(username);
            }
        }
        log.debug("reservation enrichment: auth usernames pass completado reservas={} [requestId={}]", dtos.size(), rid);
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
