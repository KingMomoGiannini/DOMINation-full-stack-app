package com.gianniniseba.authservice.dto;

/**
 * Conteos globales de solicitudes de prestador (sin paginar).
 */
public record ProviderRequestSummaryDto(long total, long pending, long approved, long rejected) {}
