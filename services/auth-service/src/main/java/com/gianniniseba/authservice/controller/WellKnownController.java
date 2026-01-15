package com.gianniniseba.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WellKnownController {

    @Value("${app.security.issuer-uri:http://auth-service:9000}")
    private String issuerUri;

    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> openidConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("issuer", issuerUri);
        config.put("authorization_endpoint", issuerUri + "/oauth2/authorize");
        config.put("token_endpoint", issuerUri + "/oauth2/token");
        config.put("jwks_uri", issuerUri + "/oauth2/jwks");
        config.put("response_types_supported", List.of("code"));
        config.put("subject_types_supported", List.of("public"));
        config.put("id_token_signing_alg_values_supported", List.of("RS256"));
        config.put("scopes_supported", List.of("openid", "profile", "read", "write"));
        return ResponseEntity.ok(config);
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public ResponseEntity<Map<String, Object>> oauthAuthorizationServer() {
        Map<String, Object> config = new HashMap<>();
        config.put("issuer", issuerUri);
        config.put("authorization_endpoint", issuerUri + "/oauth2/authorize");
        config.put("token_endpoint", issuerUri + "/oauth2/token");
        config.put("jwks_uri", issuerUri + "/oauth2/jwks");
        config.put("response_types_supported", List.of("code"));
        config.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        config.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic"));
        return ResponseEntity.ok(config);
    }
}


