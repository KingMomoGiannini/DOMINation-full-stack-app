package com.domination.booking.service;

import com.domination.booking.model.UserHandleForProviderResponse;
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

    @Value("${auth.service.url:http://localhost:9000}")
    private String authServiceUrl;

    public Optional<String> getUsernameForProvider(Long userId, String authorizationHeader) {
        String rid = Optional.ofNullable(MDC.get("requestId")).orElse("-");
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.debug("auth enrichment: omitido userId={} sin header Authorization [requestId={}]", userId, rid);
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
                return Optional.of(body.getUsername().trim());
            }
            log.info("auth enrichment: respuesta sin username userId={} [requestId={}]", userId, rid);
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                log.warn("auth enrichment: userId={} httpStatus={} (token ausente, inválido o sin rol PROVIDER) [requestId={}]",
                        userId, code, rid);
            } else if (code == 404) {
                log.info("auth enrichment: usuario 404 userId={} [requestId={}]", userId, rid);
            } else {
                log.warn("auth enrichment: userId={} httpStatus={} [requestId={}] — {}", userId, code, rid, ex.getMessage());
            }
        } catch (ResourceAccessException ex) {
            log.warn("auth enrichment: userId={} reason=timeout_or_network [requestId={}] — {}", userId, rid, ex.getMessage());
        } catch (RestClientException ex) {
            log.warn("auth enrichment: userId={} [requestId={}] — {}", userId, rid, ex.getMessage());
        }
        return Optional.empty();
    }
}
