package com.domination.booking.controller;

import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador de reservas - Todos los endpoints requieren JWT
 */
@RestController
@RequestMapping("/api/booking")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "API de reservas (requiere JWT)")
@SecurityRequirement(name = "bearer-jwt")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my/reservations")
    @Operation(summary = "Obtener mis reservas", description = "Lista las reservas del usuario autenticado")
    public ResponseEntity<List<ReservationDTO>> getMyReservations(@AuthenticationPrincipal Jwt jwt) {
        String customerId = jwt.getSubject(); // Obtener el ID del usuario desde el JWT
        return ResponseEntity.ok(reservationService.getMyReservations(customerId));
    }

    @PostMapping("/reservations")
    @Operation(summary = "Crear reserva", description = "Crea una nueva reserva para el usuario autenticado")
    public ResponseEntity<ReservationDTO> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String customerId = jwt.getSubject();
        ReservationDTO created = reservationService.createReservation(request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

