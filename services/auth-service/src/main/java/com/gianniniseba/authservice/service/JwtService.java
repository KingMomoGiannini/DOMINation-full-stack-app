package com.gianniniseba.authservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.gianniniseba.authservice.entity.User;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

/*
 * Servicio JWT anterior - Reemplazado por OAuth2TokenService
 * Comentado para evitar que Spring intente crear el bean y busque jwt.secret
 */
// @Service
public class JwtService {

    // @Value("${jwt.secret}")
    private String secret;

    // @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    private Algorithm getAlgorithm(){
        return Algorithm.HMAC256(secret);
    }

    public String generateToken(User user){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(","));

        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("roles", roles)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(getAlgorithm());
    }

    public String extractUsername(String token){
        DecodedJWT decoded = verifyToken(token);
        return decoded.getSubject();
    }

    public String extractRoles(String token){
        DecodedJWT decoded = verifyToken(token);
        return decoded.getClaim("roles").asString();
    }

    public boolean isTokedValid(String token){
        try{
            verifyToken(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private DecodedJWT verifyToken(String token){
        return JWT.require(getAlgorithm())
                .build()
                .verify(token);
    }
}
