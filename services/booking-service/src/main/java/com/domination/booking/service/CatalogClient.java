package com.domination.booking.service;

import com.domination.booking.model.ItemDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente para comunicarse con el catalog-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogClient {

    private final RestClient restClient;

    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    /**
     * Obtiene los detalles de un item desde catalog-service
     */
    public ItemDetailResponse getItemDetail(Long itemId) {
        log.debug("Consultando item {} desde catalog-service", itemId);
        
        String url = catalogServiceUrl + "/api/catalog/items/" + itemId;
        
        try {
            ItemDetailResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ItemDetailResponse.class);
            
            log.debug("Item {} obtenido: type={}, rentalMode={}, basePrice={}, qty={}", 
                    itemId, response.getType(), response.getRentalMode(), 
                    response.getBasePrice(), response.getQuantityTotal());
            
            return response;
        } catch (Exception e) {
            log.error("Error al consultar item {} desde catalog-service", itemId, e);
            throw new RuntimeException("No se pudo obtener el item " + itemId + " del catálogo", e);
        }
    }
}


