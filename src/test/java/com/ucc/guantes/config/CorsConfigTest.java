package com.ucc.guantes.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    @RestController
    static class PingTestController {
        @GetMapping("/api/auth/ping")
        public ResponseEntity<String> ping() {
            return ResponseEntity.ok("ok");
        }
    }

    private MockMvc crearMockMvcConOrigines(String allowedOrigins) {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", allowedOrigins);
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        return MockMvcBuilders.standaloneSetup(new PingTestController())
                .addFilters(new CorsFilter(source))
                .build();
    }

    @Test
    @DisplayName("CorsConfig no debe implementar WebMvcConfigurer (única fuente CorsConfigurationSource)")
    void noImplementaWebMvcConfigurer() {
        assertFalse(WebMvcConfigurer.class.isAssignableFrom(CorsConfig.class));
    }

    @Test
    @DisplayName("Origen no autorizado https://evil.example no recibe Access-Control-Allow-Origin")
    void rechazaOrigenNoAutorizado() throws Exception {
        MockMvc mockMvc = crearMockMvcConOrigines("http://localhost:5173");

        mockMvc.perform(get("/api/auth/ping")
                        .header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mockMvc.perform(options("/api/auth/ping")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Origen permitido http://localhost:5173 recibe Access-Control-Allow-Origin exacto")
    void permiteOrigenConfigurado() throws Exception {
        MockMvc mockMvc = crearMockMvcConOrigines("http://localhost:5173");

        mockMvc.perform(get("/api/auth/ping")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Lanza IllegalStateException si se intenta usar comodín '*' con allowCredentials=true")
    void rechazaComodinConCredenciales() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "http://localhost:5173,*");

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsConfig::corsConfigurationSource);
        assertTrue(ex.getMessage().contains("*"));
    }
}
