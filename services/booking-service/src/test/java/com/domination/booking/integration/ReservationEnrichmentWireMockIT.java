package com.domination.booking.integration;

import com.domination.booking.config.RestClientConfig;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import com.domination.booking.observability.ReservationEnrichmentMetrics;
import com.domination.booking.service.AuthClient;
import com.domination.booking.service.CatalogClient;
import com.domination.booking.service.ReservationDtoEnricher;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato HTTP del enriquecimiento tolerante: booking → catálogo/auth vía WireMock (sin levantar microservicios).
 */
@SpringBootTest(classes = {
        RestClientConfig.class,
        CatalogClient.class,
        AuthClient.class,
        ReservationDtoEnricher.class,
        ReservationEnrichmentMetrics.class,
        ReservationEnrichmentWireMockIT.TestMeterRegistryConfig.class
})
class ReservationEnrichmentWireMockIT {

    private static WireMockServer wireMock;

    @Configuration
    static class TestMeterRegistryConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @DynamicPropertySource
    static synchronized void registerWiremockBaseUrls(DynamicPropertyRegistry registry) {
        if (wireMock == null || !wireMock.isRunning()) {
            if (wireMock != null) {
                wireMock.stop();
            }
            wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
            wireMock.start();
        }
        String base = wireMock.baseUrl();
        registry.add("catalog.service.url", () -> base);
        registry.add("auth.service.url", () -> base);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
            wireMock = null;
        }
    }

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private ReservationDtoEnricher enricher;

    @Autowired
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
        meterRegistry.clear();
        MDC.clear();
    }

    @Test
    void catalogBranch_success_incrementsMetric_andReturnsName() {
        wireMock.stubFor(get(urlEqualTo("/api/catalog/branches/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Sede WM\"}")));

        assertEquals("Sede WM", catalogClient.fetchBranchNameForEnrichment(1L).orElseThrow());
        assertEquals(1.0,
                meterRegistry.counter("booking.enrichment.catalog", "resource", "branch", "outcome", "success").count(),
                0.001);
    }

    @Test
    void catalogBranch_http404_recordsMetric_andReturnsEmpty() {
        wireMock.stubFor(get(urlEqualTo("/api/catalog/branches/2"))
                .willReturn(aResponse().withStatus(404)));

        assertTrue(catalogClient.fetchBranchNameForEnrichment(2L).isEmpty());
        assertEquals(1.0,
                meterRegistry.counter("booking.enrichment.catalog", "resource", "branch", "outcome", "http_404").count(),
                0.001);
    }

    @Test
    void enrichMissingCatalogFields_callsCatalog_whenSnapshotBlank() {
        wireMock.stubFor(get(urlEqualTo("/api/catalog/branches/10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Branch X\"}")));
        wireMock.stubFor(get(urlEqualTo("/api/catalog/items/20"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Item Y\"}")));

        ReservationLineDTO line = ReservationLineDTO.builder()
                .id(1L).itemId(20L).quantity(1).itemName(null).build();
        ReservationDTO dto = ReservationDTO.builder()
                .id(1L).customerId("5").branchId(10L).branchName(null)
                .lines(List.of(line))
                .build();

        enricher.enrichMissingCatalogFields(List.of(dto));

        assertEquals("Branch X", dto.getBranchName());
        assertEquals("Item Y", line.getItemName());
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/catalog/branches/10")));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/catalog/items/20")));
    }

    @Test
    void propagatesXRequestId_toCatalog() {
        wireMock.stubFor(get(urlEqualTo("/api/catalog/branches/99"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"Z\"}")));

        MDC.put("requestId", "trace-it-42");
        try {
            catalogClient.fetchBranchNameForEnrichment(99L);
        } finally {
            MDC.clear();
        }

        wireMock.verify(getRequestedFor(urlEqualTo("/api/catalog/branches/99"))
                .withHeader("X-Request-Id", equalTo("trace-it-42")));
    }

    @Test
    void enrichCustomerUsernames_callsAuthHandleEndpoint() {
        wireMock.stubFor(get(urlPathEqualTo("/users/7/handle-for-provider"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"userId\":7,\"username\":\"cliente_wm\"}")));

        ReservationDTO dto = ReservationDTO.builder()
                .id(1L).customerId("7").branchId(1L).branchName("B").build();

        enricher.enrichCustomerUsernamesForProvider(List.of(dto), "Bearer test-token");

        assertEquals("cliente_wm", dto.getCustomerUsername());
        wireMock.verify(getRequestedFor(urlPathEqualTo("/users/7/handle-for-provider")));
        assertTrue(meterRegistry.counter("booking.enrichment.auth", "resource", "handle", "outcome", "success").count() >= 1);
    }
}
