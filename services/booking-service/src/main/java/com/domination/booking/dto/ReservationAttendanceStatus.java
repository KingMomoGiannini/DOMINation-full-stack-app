package com.domination.booking.dto;

/**
 * Vista operativa derivada solo a partir de hechos persistidos de asistencia.
 * No depende del reloj del servidor.
 */
public enum ReservationAttendanceStatus {
    NOT_RECORDED,
    CHECKED_IN,
    NO_SHOW,
    NOT_APPLICABLE
}
