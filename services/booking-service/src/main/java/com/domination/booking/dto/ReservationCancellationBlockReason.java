package com.domination.booking.dto;

/**
 * Motivo derivado por el cual la reserva ya no puede cancelarse.
 */
public enum ReservationCancellationBlockReason {
    ALREADY_CANCELLED,
    ALREADY_STARTED
}
