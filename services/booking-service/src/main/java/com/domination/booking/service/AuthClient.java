package com.domination.booking.service;

import com.domination.booking.model.UserHandleForProviderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        String url = authServiceUrl + "/users/" + userId + "/handle-for-provider";
        try {
            UserHandleForProviderResponse body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(UserHandleForProviderResponse.class);
            if (body != null && body.getUsername() != null) {
                return Optional.of(body.getUsername());
            }
        } catch (RestClientException ex) {
            log.warn("No se pudo obtener username para userId={} desde auth-service: {}", userId, ex.getMessage());
        }
        return Optional.empty();
    }
}
