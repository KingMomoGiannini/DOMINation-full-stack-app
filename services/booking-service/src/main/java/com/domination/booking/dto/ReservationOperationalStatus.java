package com.domination.booking.dto;

/**
 * Estado operativo derivado para lectura/UI.
 * No se persiste: se calcula a partir de status + tiempo actual.
 */
public enum ReservationOperationalStatus {
    UPCOMING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
