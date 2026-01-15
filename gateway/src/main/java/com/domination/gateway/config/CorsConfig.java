package com.domination.gateway.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration cfg = new CorsConfiguration();

    // Si vas a mandar cookies -> true. Si solo JWT en header, puede ser false.
    cfg.setAllowCredentials(true);

    // Orígenes explícitos (más seguro y evita problemas con allowCredentials)
    cfg.setAllowedOrigins(List.of(
        "http://localhost:5173",
        "http://127.0.0.1:5173"
    ));

    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

    // IMPORTANTE: Authorization y Content-Type para JWT + JSON
    cfg.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "Accept",
        "Origin",
        "X-Requested-With"
    ));

    // Si necesitás leer headers desde el front (opcional)
    cfg.setExposedHeaders(List.of("Location"));

    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);

    return new CorsWebFilter(source);
  }
}
