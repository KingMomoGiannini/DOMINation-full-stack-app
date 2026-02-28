package com.domination.booking.controller;

import com.domination.booking.dto.AvailabilityResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my/reservations")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Obtener mis reservas", description = "Lista las reservas del usuario autenticado (ROLE_USER)")
    public ResponseEntity<List<ReservationDTO>> getMyReservations(@AuthenticationPrincipal Jwt jwt) {
        String customerId = String.valueOf(extractUserId(jwt));
        return ResponseEntity.ok(reservationService.getMyReservations(customerId));
    }

    @GetMapping("/provider/reservations")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Obtener reservas de mis sucursales", description = "Lista las reservas de las sucursales del provider (ROLE_PROVIDER)")
    public ResponseEntity<List<ReservationDTO>> getProviderReservations(@AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractUserId(jwt);
        return ResponseEntity.ok(reservationService.getProviderReservations(providerId));
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Crear reserva", description = "Crea una nueva reserva para el usuario autenticado (ROLE_USER)")
    public ResponseEntity<ReservationDTO> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String customerId = String.valueOf(extractUserId(jwt));
        ReservationDTO created = reservationService.createReservation(request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/availability")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Pre-check de disponibilidad", description = "Valida disponibilidad sin persistir reservas (ROLE_USER)")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String customerId = String.valueOf(extractUserId(jwt));
        AvailabilityResponse response = reservationService.checkAvailability(request, customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae userId del JWT (puede venir como Integer, Long o String)
     */
    private Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim == null) {
            throw new IllegalStateException("userId no encontrado en JWT");
        }
        if (userIdClaim instanceof Number) {
            return ((Number) userIdClaim).longValue();
        }
        if (userIdClaim instanceof String) {
            return Long.parseLong((String) userIdClaim);
        }
        throw new IllegalStateException("userId tiene un tipo no soportado: " + userIdClaim.getClass());
    }

    /**
     * endpoint de cancelación
     * Saca customerId del JWT (normalmente jwt.getSubject() o claim "sub").
     * Llama service.cancel(id, customerId).
     * Devuelve ReservationDTO con mapper.
     */
    @PostMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ReservationDTO cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String customerId = String.valueOf(extractUserId(jwt));
        return reservationService.cancelReservation(id,customerId);

    }
}

