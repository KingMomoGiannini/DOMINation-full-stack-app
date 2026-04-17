package com.domination.booking.dto;

/**
 * Motivos derivados por los cuales una acción operativa del prestador no está disponible.
 */
public enum ReservationProviderActionBlockReason {
    CANCELLED,
    ALREADY_CHECKED_IN,
    ALREADY_MARKED_NO_SHOW,
    BEFORE_START,
    AFTER_END,
    BEFORE_END
}
