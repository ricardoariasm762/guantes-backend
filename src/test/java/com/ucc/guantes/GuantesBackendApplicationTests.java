package com.ucc.guantes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Prueba de integración de contexto completo: requiere instancia de PostgreSQL 'guantes_db' activa en localhost:5432")
class GuantesBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
