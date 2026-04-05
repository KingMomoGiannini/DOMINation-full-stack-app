package com.domination.booking.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Contadores de enriquecimiento de listados (catálogo / auth). Outcomes estables para alertas y dashboards.
 */
@Component
public class ReservationEnrichmentMetrics {

    private final MeterRegistry registry;

    public ReservationEnrichmentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void catalog(String resource, String outcome) {
        registry.counter("booking.enrichment.catalog", "resource", resource, "outcome", outcome).increment();
    }

    public void auth(String outcome) {
        registry.counter("booking.enrichment.auth", "resource", "handle", "outcome", outcome).increment();
    }
}
