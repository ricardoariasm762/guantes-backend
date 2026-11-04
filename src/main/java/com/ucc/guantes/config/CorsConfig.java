package com.ucc.guantes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    private List<String> parseAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalStateException(
                    "Configuración inválida: 'app.cors.allowed-origins' no puede estar vacío."
            );
        }
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "Configuración inválida: debe especificarse al menos un origen en 'app.cors.allowed-origins'."
            );
        }
        return origins;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        boolean allowCredentials = true;
        List<String> origins = parseAllowedOrigins();

        if (allowCredentials && origins.stream().anyMatch(origin -> "*".equals(origin) || origin.contains("*"))) {
            throw new IllegalStateException(
                    "Configuración CORS insegura: no se permite el comodín '*' en 'app.cors.allowed-origins' cuando allowCredentials es true."
            );
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
