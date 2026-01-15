package com.domination.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad OAuth2 Resource Server
 * 
 * ENDPOINTS PÚBLICOS:
 * - GET /api/catalog/branches/** (listar sucursales)
 * - GET /api/catalog/items/** (listar items)
 * - /swagger-ui/**, /v3/api-docs/**, /actuator/**
 * 
 * ENDPOINTS PROTEGIDOS:
 * - /api/catalog/provider/** -> @PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
 * - /api/catalog/admin/** -> hasRole("ADMIN")
 * 
 * JWT CLAIMS:
 * - authorities: ["ROLE_PROVIDER", "ROLE_USER", ...]
 * - userId: <number>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configure(http))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos - GET solamente
                .requestMatchers(HttpMethod.GET, "/api/catalog/branches/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/items/**").permitAll()
                
                // Swagger & Actuator públicos
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                
                // Admin endpoints requieren ROLE_ADMIN (por URL)
                .requestMatchers("/api/catalog/admin/**").hasRole("ADMIN")
                
                // Provider endpoints (protegidos por @PreAuthorize en controller)
                .requestMatchers("/api/catalog/provider/**").authenticated()
                
                // Cualquier otra request requiere autenticación
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Convierte el JWT en Authentication con GrantedAuthorities
     * Lee el claim "authorities" del JWT y lo convierte a roles de Spring Security
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Configurar el converter de authorities
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Leer desde claim "authorities" (default es "scope")
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        
        // NO agregar prefijo (el JWT ya tiene "ROLE_")
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        
        // Crear el converter principal
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


