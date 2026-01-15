package com.gianniniseba.authservice.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JWKSource<SecurityContext> jwkSource;

    @GetMapping("/oauth2/jwks")
    public ResponseEntity<Map<String, Object>> jwks() {
        try {
            // Obtener todas las claves del JWKSource
            JWKSet jwkSet = new JWKSet(jwkSource.get(null, null));
            return ResponseEntity.ok(jwkSet.toJSONObject());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar JWKS", e);
        }
    }
}

