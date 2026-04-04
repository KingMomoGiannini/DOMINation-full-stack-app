package com.domination.booking.service;

import com.domination.booking.model.BranchResponse;
import com.domination.booking.model.HoldInventoryRequest;
import com.domination.booking.model.HoldInventoryResponse;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.model.ReleaseInventoryRequest;
import com.domination.booking.model.ReleaseInventoryResponse;
import com.domination.booking.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    /**
     * Obtiene los detalles de un branch desde catalog-service
     */
    public BranchResponse getBranchDetail(Long branchId) {
        log.debug("Consultando branch {} desde catalog-service", branchId);
        
        String url = catalogServiceUrl + "/api/catalog/branches/" + branchId;
        
        try {
            BranchResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(BranchResponse.class);
            
            log.debug("Branch {} obtenido: name={}, providerId={}", 
                    branchId, response.getName(), response.getProviderId());
            
            return response;
        } catch (Exception e) {
            log.error("Error al consultar branch {} desde catalog-service", branchId, e);
            throw new RuntimeException("No se pudo obtener el branch " + branchId + " del catálogo", e);
        }
    }

    public HoldInventoryResponse holdInventory(HoldInventoryRequest request) {
        log.debug("Solicitando hold de inventario: itemId={}, quantity={}", request.getItemId(), request.getQuantity());

        String url = catalogServiceUrl + "/api/catalog/inventory/hold";
        try {
            HoldInventoryResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(HoldInventoryResponse.class);
            log.debug("Hold creado para item {}: holdId={}", request.getItemId(), response != null ? response.getHoldId() : null);
            return response;
        } catch (HttpClientErrorException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            if (statusCode.value() == 409) {
                throw new ConflictException(extractCatalogErrorMessage(ex, "Conflicto de inventario"));
            }
            throw ex;
        } catch (Exception e) {
            log.error("Error al solicitar hold para item {}", request.getItemId(), e);
            throw new RuntimeException("No se pudo crear hold de inventario para item " + request.getItemId(), e);
        }
    }

    public ReleaseInventoryResponse releaseInventory(ReleaseInventoryRequest request) {
        log.debug("Solicitando release de holdId={}", request.getHoldId());

        String url = catalogServiceUrl + "/api/catalog/inventory/release";
        try {
            ReleaseInventoryResponse response = restClient.post()
                    .uri(url)
                    .body(request)
                    .retrieve()
                    .body(ReleaseInventoryResponse.class);
            log.debug("Release ejecutado para holdId={}, released={}",
                    request.getHoldId(), response != null && response.isReleased());
            return response;
        } catch (Exception e) {
            log.error("Error al liberar holdId={}", request.getHoldId(), e);
            throw new RuntimeException("No se pudo liberar holdId=" + request.getHoldId(), e);
        }
    }

    private String extractCatalogErrorMessage(HttpClientErrorException ex, String fallback) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            // Intenta extraer "detail" del ProblemDetail sin acoplarse a un parser JSON.
            String marker = "\"detail\":\"";
            int start = body.indexOf(marker);
            if (start >= 0) {
                int valueStart = start + marker.length();
                int end = body.indexOf("\"", valueStart);
                if (end > valueStart) {
                    return body.substring(valueStart, end);
                }
            }
        }
        return fallback;
    }
}


