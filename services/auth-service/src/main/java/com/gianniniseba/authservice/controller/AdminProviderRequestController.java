package com.gianniniseba.authservice.controller;

import com.gianniniseba.authservice.entity.ProviderRequest;
import com.gianniniseba.authservice.service.ProviderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/provider-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProviderRequestController {

    private final ProviderRequestService providerRequestService;

    @GetMapping
    public ResponseEntity<List<ProviderRequest>> getProviderRequests(
            @RequestParam(required = false) ProviderRequest.RequestStatus status) {
        List<ProviderRequest> requests = providerRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(requests);
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

