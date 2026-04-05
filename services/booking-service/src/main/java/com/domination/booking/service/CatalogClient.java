package com.domination.booking.service;

import com.domination.booking.model.BranchResponse;
import com.domination.booking.model.HoldInventoryRequest;
import com.domination.booking.model.HoldInventoryResponse;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.model.ReleaseInventoryRequest;
import com.domination.booking.model.ReleaseInventoryResponse;
import com.domination.booking.exception.ConflictException;
import com.domination.booking.observability.ReservationEnrichmentMetrics;
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
    private final ReservationEnrichmentMetrics enrichmentMetrics;

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
                enrichmentMetrics.catalog("branch", "success");
                return Optional.of(response.getName().trim());
            }
            enrichmentMetrics.catalog("branch", "empty_payload");
            log.info("[enrichment] dependency=catalog resource=branch outcome=empty_payload branchId={} requestId={}",
                    branchId, rid);
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404) {
                enrichmentMetrics.catalog("branch", "http_404");
                log.info("[enrichment] dependency=catalog resource=branch outcome=http_404 branchId={} "
                                + "hint=legacy_missing_snapshot_or_deleted requestId={}",
                        branchId, rid);
            } else {
                String outcome = code >= 500 ? "http_5xx" : "http_4xx";
                enrichmentMetrics.catalog("branch", outcome);
                log.warn("[enrichment] dependency=catalog resource=branch outcome={} branchId={} httpStatus={} requestId={} msg={}",
                        outcome, branchId, code, rid, ex.getMessage());
            }
            return Optional.empty();
        } catch (ResourceAccessException ex) {
            enrichmentMetrics.catalog("branch", "timeout");
            log.warn("[enrichment] dependency=catalog resource=branch outcome=timeout branchId={} requestId={} msg={}",
                    branchId, rid, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            enrichmentMetrics.catalog("branch", "unexpected");
            log.warn("[enrichment] dependency=catalog resource=branch outcome=unexpected branchId={} requestId={}",
                    branchId, rid, ex);
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
                enrichmentMetrics.catalog("item", "success");
                return Optional.of(response.getName().trim());
            }
            enrichmentMetrics.catalog("item", "empty_payload");
            log.info("[enrichment] dependency=catalog resource=item outcome=empty_payload itemId={} requestId={}",
                    itemId, rid);
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 404) {
                enrichmentMetrics.catalog("item", "http_404");
                log.info("[enrichment] dependency=catalog resource=item outcome=http_404 itemId={} "
                                + "hint=legacy_missing_snapshot_or_deleted requestId={}",
                        itemId, rid);
            } else {
                String outcome = code >= 500 ? "http_5xx" : "http_4xx";
                enrichmentMetrics.catalog("item", outcome);
                log.warn("[enrichment] dependency=catalog resource=item outcome={} itemId={} httpStatus={} requestId={} msg={}",
                        outcome, itemId, code, rid, ex.getMessage());
            }
            return Optional.empty();
        } catch (ResourceAccessException ex) {
            enrichmentMetrics.catalog("item", "timeout");
            log.warn("[enrichment] dependency=catalog resource=item outcome=timeout itemId={} requestId={} msg={}",
                    itemId, rid, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            enrichmentMetrics.catalog("item", "unexpected");
            log.warn("[enrichment] dependency=catalog resource=item outcome=unexpected itemId={} requestId={}",
                    itemId, rid, ex);
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


