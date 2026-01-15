package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenService.class);

    private final JwtEncoder jwtEncoder;

    @Value("${app.security.issuer-uri:http://auth-service:9000}")
    private String issuerUri;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600); // 1 hora

        // Convertir roles a lista de authorities (formato esperado por Resource Servers)
        var authorities = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        // Log del issuer que se está usando (para debug)
        logger.info("Generando token JWT para usuario: {} con issuer: {} (desde property: app.security.issuer-uri)", 
                user.getUsername(), issuerUri);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUri)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getUsername())
                .claim("scope", "read write openid profile")
                .claim("authorities", authorities)  // Lista de strings ["ROLE_ADMIN", "ROLE_USER"]
                .claim("userId", user.getId())      // ID del usuario para auditoría
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        logger.debug("Token JWT generado exitosamente para usuario: {}", user.getUsername());
        return token;
    }
}
