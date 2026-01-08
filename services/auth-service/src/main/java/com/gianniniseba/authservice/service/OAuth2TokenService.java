package com.gianniniseba.authservice.service;

import com.gianniniseba.authservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuth2TokenService {

    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600); // 1 hora

        String authorities = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8080")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getUsername())
                .claim("scope", "read write openid profile")
                .claim("authorities", authorities)
                .claim("roles", authorities)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

