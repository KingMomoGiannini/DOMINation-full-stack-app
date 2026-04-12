package com.domination.booking.domain;

/**
 * Estados persistidos de una reserva.
 * El momento operativo (proxima / en curso / finalizada) se deriva aparte.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
