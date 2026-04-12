package com.gianniniseba.authservice.controller;

import com.gianniniseba.authservice.dto.ProviderRequestSummaryDto;
import com.gianniniseba.authservice.entity.ProviderRequest;
import com.gianniniseba.authservice.service.ProviderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/provider-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderRequestController {

    private final ProviderRequestService providerRequestService;

    /**
     * Listado paginado. Filtros opcionales: estado exacto, userId exacto. Orden por {@code createdAt} asc/desc.
     */
    @GetMapping
    public ResponseEntity<Page<ProviderRequest>> getProviderRequests(
            @RequestParam(required = false) ProviderRequest.RequestStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sortSpec = parseCreatedAtSort(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, sortSpec);
        Page<ProviderRequest> result = providerRequestService.findRequestsForAdmin(status, userId, pageable);
        return ResponseEntity.ok(result);
    }

    /** Conteos globales para panel admin (una lectura liviana por carga de pantalla). */
    @GetMapping("/summary")
    public ResponseEntity<ProviderRequestSummaryDto> getSummary() {
        return ResponseEntity.ok(providerRequestService.getAdminSummary());
    }

    private static Sort parseCreatedAtSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return createdAtSort(Sort.Direction.DESC);
        }
        String[] parts = sort.split(",", 2);
        String prop = parts[0].trim();
        if (!"createdAt".equalsIgnoreCase(prop)) {
            return createdAtSort(Sort.Direction.DESC);
        }
        Sort.Direction dir = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            dir = Sort.Direction.ASC;
        }
        return createdAtSort(dir);
    }

    private static Sort createdAtSort(Sort.Direction dir) {
        return Sort.by(dir, "createdAt").and(Sort.by(dir, "id"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveProviderRequest(@PathVariable Long id) {
        try {
            providerRequestService.approveRequest(id);
            return ResponseEntity.ok(Map.of("message", "Solicitud aprobada exitosamente"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectProviderRequest(@PathVariable Long id) {
        try {
            providerRequestService.rejectRequest(id);
            return ResponseEntity.ok(Map.of("message", "Solicitud rechazada"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

