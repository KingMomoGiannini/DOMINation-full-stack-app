package com.gianniniseba.authservice.exception;

import com.gianniniseba.authservice.dto.AuthResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("detail", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<AuthResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        AuthResponse response = AuthResponse.builder()
                .message(ex.getMessage())
                .token(null)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<AuthResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        AuthResponse response = AuthResponse.builder()
                .message(ex.getMessage())
                .token(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
    }
}
