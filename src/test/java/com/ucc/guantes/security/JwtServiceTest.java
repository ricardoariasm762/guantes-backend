package com.ucc.guantes.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtService.class);

    @Test
    @DisplayName("Debe fallar al arrancar si JWT_SECRET falta o no se resuelve")
    void debeFallarSiFaltaJwtSecret() {
        contextRunner
                .withPropertyValues("jwt.secret=")
                .run(context -> {
                    assertNotNull(context.getStartupFailure(), "El contexto debe fallar si jwt.secret está vacío");
                    assertTrue(
                            context.getStartupFailure().getMessage().contains("JWT_SECRET")
                                    || (context.getStartupFailure().getCause() != null
                                    && context.getStartupFailure().getCause().getMessage().contains("JWT_SECRET"))
                    );
                });
    }

    @Test
    @DisplayName("Debe fallar al arrancar si JWT_SECRET tiene menos de 32 bytes")
    void debeFallarSiJwtSecretEsMenorA32Bytes() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "clave_demasiado_corta_123");

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtService::validarConfiguracion);
        assertTrue(ex.getMessage().contains("32 bytes"));
    }

    @Test
    @DisplayName("Debe iniciar correctamente cuando JWT_SECRET tiene 32 bytes o más")
    void debeIniciarConSecretoValido() {
        contextRunner
                .withPropertyValues("jwt.secret=secreto_de_prueba_valido_con_al_menos_32_bytes_exactos")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(JwtService.class));
                });
    }
}
