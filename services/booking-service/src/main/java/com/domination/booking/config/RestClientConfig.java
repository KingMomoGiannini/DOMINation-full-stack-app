package com.domination.booking.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient compartido por {@link com.domination.booking.service.CatalogClient} y
 * {@link com.domination.booking.service.AuthClient}: timeouts acotados y correlación hacia dependencias.
 */
@Configuration
public class RestClientConfig {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";

    @Bean
    public RestClient restClient(
            @Value("${booking.http-client.connect-timeout-ms:2500}") int connectTimeoutMs,
            @Value("${booking.http-client.read-timeout-ms:8000}") int readTimeoutMs) {

        ClientHttpRequestFactory requestFactory = buildRequestFactory(connectTimeoutMs, readTimeoutMs);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String rid = MDC.get(MDC_REQUEST_ID);
                    if (rid != null && !rid.isBlank()) {
                        request.getHeaders().add(REQUEST_ID_HEADER, rid);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private static ClientHttpRequestFactory buildRequestFactory(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
