package com.gianniniseba.authservice.controller;

import com.gianniniseba.authservice.entity.ProviderRequest;
import com.gianniniseba.authservice.service.ProviderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/provider-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ProviderRequestController {

    private final ProviderRequestService providerRequestService;

    @PostMapping
    public ResponseEntity<?> createProviderRequest(@AuthenticationPrincipal Jwt jwt) {
        try {
            Long userId = extractUserId(jwt);
            ProviderRequest request = providerRequestService.createRequest(userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Solicitud creada exitosamente", "id", request.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyRequest(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        ProviderRequest request = providerRequestService.getMyRequest(userId);
        if (request == null) {
            return ResponseEntity.ok(Map.of("message", "No tienes solicitudes"));
        }
        return ResponseEntity.ok(request);
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
        throw new IllegalStateException("userId tiene un tipo no soportado");
    }
}

