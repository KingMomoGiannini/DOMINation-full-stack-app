package com.domination.booking.dto;

/**
 * Conteos de reservas del prestador (sin paginar).
 */
public record ProviderReservationMetricsDto(
        long total,
        long cancelled,
        long upcoming,
        long inProgress,
        long completed,
        long checkedIn,
        long noShow) {}
