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
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

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

    /**
     * Lectura tolerante a fallos para enriquecer listados: no propaga excepciones.
     */
    public Optional<String> fetchBranchNameForEnrichment(Long branchId) {
        if (branchId == null) {
            return Optional.empty();
        }
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        String url = catalogServiceUrl + "/api/catalog/branches/" + branchId;
        try {
            BranchResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(BranchResponse.class);
            if (response != null && response.getName() != null && !response.getName().isBlank()) {
                return Optional.of(response.getName().trim());
            }
            log.info("catalog enrichment: nombre vacío branchId={} [requestId={}]", branchId, rid);
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404) {
                log.info("catalog enrichment: branch 404 branchId={} (sin snapshot persistido o recurso ausente) [requestId={}]", branchId, rid);
            } else {
                log.warn("catalog enrichment: branch branchId={} httpStatus={} [requestId={}] — {}", branchId, code, rid, ex.getMessage());
            }
            return Optional.empty();
        } catch (ResourceAccessException ex) {
            log.warn("catalog enrichment: branch branchId={} reason=timeout_or_network [requestId={}] — {}", branchId, rid, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("catalog enrichment: branch branchId={} reason=unexpected [requestId={}]", branchId, rid, ex);
            return Optional.empty();
        }
    }

    /**
     * Lectura tolerante a fallos para enriquecer listados: no propaga excepciones.
     */
    public Optional<String> fetchItemNameForEnrichment(Long itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        String url = catalogServiceUrl + "/api/catalog/items/" + itemId;
        try {
            ItemDetailResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(ItemDetailResponse.class);
            if (response != null && response.getName() != null && !response.getName().isBlank()) {
                return Optional.of(response.getName().trim());
            }
            log.info("catalog enrichment: nombre vacío itemId={} [requestId={}]", itemId, rid);
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404) {
                log.info("catalog enrichment: item 404 itemId={} (sin snapshot persistido o recurso ausente) [requestId={}]", itemId, rid);
            } else {
                log.warn("catalog enrichment: item itemId={} httpStatus={} [requestId={}] — {}", itemId, code, rid, ex.getMessage());
            }
            return Optional.empty();
        } catch (ResourceAccessException ex) {
            log.warn("catalog enrichment: item itemId={} reason=timeout_or_network [requestId={}] — {}", itemId, rid, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("catalog enrichment: item itemId={} reason=unexpected [requestId={}]", itemId, rid, ex);
            return Optional.empty();
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


