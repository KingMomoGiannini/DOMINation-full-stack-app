package com.domination.booking.exception;

/**
 * Excepción para stock insuficiente
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}


