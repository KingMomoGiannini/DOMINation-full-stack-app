package com.gianniniseba.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerConfig {

    private final JwtDecoder jwtDecoder;

    public OAuth2ResourceServerConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .securityMatcher("/auth/login", "/auth/register", "/actuator/**")
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                .securityMatcher("/users/**", "/admin/**", "/auth/provider-requests/**", "/test/**")
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/auth/provider-requests/**").authenticated()
                        .requestMatchers("/users/**").authenticated()
                        .requestMatchers("/test/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extraemos los roles del claim "authorities" o "roles"
            Object authoritiesClaim = jwt.getClaim("authorities");
            if (authoritiesClaim != null) {
                if (authoritiesClaim instanceof String) {
                    String authoritiesStr = (String) authoritiesClaim;
                    return java.util.stream.Stream.of(authoritiesStr.split("\\s+"))
                            .filter(auth -> !auth.isEmpty())
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                } else if (authoritiesClaim instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> authoritiesList = (List<String>) authoritiesClaim;
                    return authoritiesList.stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                }
            }
            
            // Si no hay authorities, intentamos con "roles"
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim != null) {
                if (rolesClaim instanceof String) {
                    String rolesStr = (String) rolesClaim;
                    return java.util.stream.Stream.of(rolesStr.split("\\s+"))
                            .filter(role -> !role.isEmpty())
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                } else if (rolesClaim instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> rolesList = (List<String>) rolesClaim;
                    return rolesList.stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                }
            }
            
            return java.util.Collections.emptyList();
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

