package com.gianniniseba.authservice.exception;

import com.gianniniseba.authservice.dto.AuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
