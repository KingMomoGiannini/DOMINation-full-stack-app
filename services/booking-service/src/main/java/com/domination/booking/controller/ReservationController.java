package com.domination.booking.controller;

import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.AvailabilityResponse;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ProviderReservationAttendanceFilter;
import com.domination.booking.dto.ProviderReservationMetricsDto;
import com.domination.booking.dto.ProviderReservationTimeMode;
import com.domination.booking.dto.ReservationAuditEventDTO;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.service.ReservationAuditService;
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
import java.util.ArrayList;
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
    private final ReservationAuditService reservationAuditService;

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
            @RequestParam(required = false, defaultValue = "ALL") String attendance,
            @RequestParam(required = false, defaultValue = "ALL") String time,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "startAt,desc") String sort) {
        Long providerId = extractUserId(jwt);
        validateWindow(from, to);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, parseStartAtSort(sort));
        Page<ReservationDTO> result = reservationService.searchProviderReservations(
                providerId,
                branchId,
                status,
                parseAttendanceFilter(attendance),
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

    @PostMapping("/provider/reservations/{id}/check-in")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Registrar check-in", description = "El prestador registra que el cliente se presentó dentro de la franja activa.")
    public ResponseEntity<ReservationDTO> providerCheckInReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Long providerId = extractUserId(jwt);
        return ResponseEntity.ok(reservationService.providerCheckInReservation(id, providerId, authorization));
    }

    @PostMapping("/provider/reservations/{id}/no-show")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Marcar no-show", description = "El prestador marca que el cliente no se presentó una vez finalizada la franja.")
    public ResponseEntity<ReservationDTO> providerMarkNoShow(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Long providerId = extractUserId(jwt);
        return ResponseEntity.ok(reservationService.providerMarkNoShow(id, providerId, authorization));
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
            return startAtSort(Sort.Direction.DESC);
        }
        String[] parts = sort.split(",", 2);
        String prop = parts[0].trim();
        if (!"startAt".equalsIgnoreCase(prop)) {
            return startAtSort(Sort.Direction.DESC);
        }
        Sort.Direction dir = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            dir = Sort.Direction.ASC;
        }
        return startAtSort(dir);
    }

    private static Sort startAtSort(Sort.Direction dir) {
        return Sort.by(dir, "startAt").and(Sort.by(dir, "id"));
    }

    private static ProviderReservationTimeMode parseTimeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProviderReservationTimeMode.ALL;
        }
        return switch (raw.trim().toUpperCase()) {
            case "UPCOMING" -> ProviderReservationTimeMode.UPCOMING;
            case "IN_PROGRESS" -> ProviderReservationTimeMode.IN_PROGRESS;
            case "COMPLETED", "PAST" -> ProviderReservationTimeMode.COMPLETED;
            default -> ProviderReservationTimeMode.ALL;
        };
    }

    private static ProviderReservationAttendanceFilter parseAttendanceFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return ProviderReservationAttendanceFilter.ALL;
        }
        return switch (raw.trim().toUpperCase()) {
            case "CHECKED_IN" -> ProviderReservationAttendanceFilter.CHECKED_IN;
            case "NO_SHOW" -> ProviderReservationAttendanceFilter.NO_SHOW;
            case "NOT_RECORDED" -> ProviderReservationAttendanceFilter.NOT_RECORDED;
            case "NOT_APPLICABLE" -> ProviderReservationAttendanceFilter.NOT_APPLICABLE;
            default -> ProviderReservationAttendanceFilter.ALL;
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

    private static void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("El parámetro 'from' no puede ser posterior a 'to'");
        }
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

    @GetMapping("/reservations/{id}/audit")
    @PreAuthorize("hasAnyRole('USER','PROVIDER','ADMIN')")
    @Operation(summary = "Auditoría operativa de reserva", description = "Lista eventos auditables de una reserva según ownership del usuario autenticado.")
    public ResponseEntity<List<ReservationAuditEventDTO>> getReservationAudit(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actorUserId = String.valueOf(extractUserId(jwt));
        return ResponseEntity.ok(reservationAuditService.getAuditForReservation(id, actorUserId, extractRoles(jwt)));
    }

    private static List<String> extractRoles(Jwt jwt) {
        Object rawAuthorities = jwt.getClaim("authorities");
        List<String> roles = new ArrayList<>();
        if (rawAuthorities instanceof Iterable<?> iterable) {
            for (Object authority : iterable) {
                if (authority != null) {
                    roles.add(String.valueOf(authority));
                }
            }
        } else if (rawAuthorities instanceof String authority && !authority.isBlank()) {
            roles.add(authority);
        }
        if (roles.isEmpty()) {
            throw new IllegalStateException("authorities no encontrado en JWT");
        }
        return roles;
    }
}

