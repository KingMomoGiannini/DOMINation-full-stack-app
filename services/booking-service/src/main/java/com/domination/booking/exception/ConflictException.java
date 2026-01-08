package com.domination.booking.exception;

/**
 * Excepción para conflictos de reserva (solapamiento de horarios)
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}


