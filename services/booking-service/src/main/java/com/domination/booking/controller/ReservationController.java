package com.domination.booking.controller;

import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.AvailabilityResponse;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ProviderReservationMetricsDto;
import com.domination.booking.dto.ProviderReservationTimeMode;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    /**
     * Listado paginado con filtros server-side. Ventana temporal opcional (solape con [from, to]).
     */
    @GetMapping("/provider/reservations")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Reservas de mis sucursales (paginado)", description = "Listado paginado; filtros por sucursal, estado, momento (ALL/UPCOMING/PAST) y ventana opcional.")
    public ResponseEntity<Page<ReservationDTO>> getProviderReservations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false, defaultValue = "ALL") String time,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "startAt,desc") String sort) {
        Long providerId = extractUserId(jwt);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, parseStartAtSort(sort));
        Page<ReservationDTO> result = reservationService.searchProviderReservations(
                providerId,
                branchId,
                status,
                parseTimeMode(time),
                from,
                to,
                pageable,
                authorization);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/provider/reservations/metrics")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Métricas de reservas del prestador", description = "Conteos totales para panel (sin paginar).")
    public ResponseEntity<ProviderReservationMetricsDto> getProviderReservationMetrics(@AuthenticationPrincipal Jwt jwt) {
        Long providerId = extractUserId(jwt);
        return ResponseEntity.ok(reservationService.getProviderReservationMetrics(providerId));
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
    private static Sort parseStartAtSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "startAt");
        }
        String[] parts = sort.split(",", 2);
        String prop = parts[0].trim();
        if (!"startAt".equalsIgnoreCase(prop)) {
            return Sort.by(Sort.Direction.DESC, "startAt");
        }
        Sort.Direction dir = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            dir = Sort.Direction.ASC;
        }
        return Sort.by(dir, "startAt");
    }

    private static ProviderReservationTimeMode parseTimeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProviderReservationTimeMode.ALL;
        }
        return switch (raw.trim().toUpperCase()) {
            case "UPCOMING" -> ProviderReservationTimeMode.UPCOMING;
            case "PAST" -> ProviderReservationTimeMode.PAST;
            default -> ProviderReservationTimeMode.ALL;
        };
    }

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

