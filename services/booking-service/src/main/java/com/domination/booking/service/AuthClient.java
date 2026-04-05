package com.domination.booking.service;

import com.domination.booking.model.UserHandleForProviderResponse;
import com.domination.booking.observability.ReservationEnrichmentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

/**
 * Cliente hacia auth-service para enriquecer listados de reservas del prestador.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthClient {

    private final RestClient restClient;
    private final ReservationEnrichmentMetrics enrichmentMetrics;

    @Value("${auth.service.url:http://localhost:9000}")
    private String authServiceUrl;

    public Optional<String> getUsernameForProvider(Long userId, String authorizationHeader) {
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            enrichmentMetrics.auth("skipped_no_authorization");
            log.debug("[enrichment] dependency=auth resource=handle outcome=skipped_no_authorization userId={} requestId={}",
                    userId, rid);
            return Optional.empty();
        }
        String url = authServiceUrl + "/users/" + userId + "/handle-for-provider";
        try {
            UserHandleForProviderResponse body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserHandleForProviderResponse.class);
            if (body != null && body.getUsername() != null && !body.getUsername().isBlank()) {
                enrichmentMetrics.auth("success");
                return Optional.of(body.getUsername().trim());
            }
            enrichmentMetrics.auth("empty_payload");
            log.info("[enrichment] dependency=auth resource=handle outcome=empty_payload userId={} requestId={}", userId, rid);
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                enrichmentMetrics.auth("http_401_403");
                log.warn("[enrichment] dependency=auth resource=handle outcome=http_401_403 userId={} httpStatus={} "
                                + "hint=token_invalid_or_not_provider requestId={}",
                        userId, code, rid);
            } else if (code == 404) {
                enrichmentMetrics.auth("http_404");
                log.info("[enrichment] dependency=auth resource=handle outcome=http_404 userId={} requestId={}",
                        userId, rid);
            } else {
                String outcome = code >= 500 ? "http_5xx" : "http_4xx";
                enrichmentMetrics.auth(outcome);
                log.warn("[enrichment] dependency=auth resource=handle outcome={} userId={} httpStatus={} requestId={} msg={}",
                        outcome, userId, code, rid, ex.getMessage());
            }
        } catch (ResourceAccessException ex) {
            enrichmentMetrics.auth("timeout");
            log.warn("[enrichment] dependency=auth resource=handle outcome=timeout userId={} requestId={} msg={}",
                    userId, rid, ex.getMessage());
        } catch (RestClientException ex) {
            enrichmentMetrics.auth("unexpected");
            log.warn("[enrichment] dependency=auth resource=handle outcome=unexpected userId={} requestId={} msg={}",
                    userId, rid, ex.getMessage());
        }
        return Optional.empty();
    }
}
