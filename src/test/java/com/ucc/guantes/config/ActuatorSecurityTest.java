package com.ucc.guantes.config;

import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.ModeloIARepository;
import com.ucc.guantes.repository.PasoCompletadoRepository;
import com.ucc.guantes.repository.ProgresoRepository;
import com.ucc.guantes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "app.cors.allowed-origins=https://d1234567890.cloudfront.net,http://localhost:5173",
        "management.endpoints.web.exposure.include=health",
        "management.endpoint.health.show-details=never"
})
class ActuatorSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private IntentoPracticaRepository intentoPracticaRepository;

    @MockitoBean
    private PasoCompletadoRepository pasoCompletadoRepository;

    @MockitoBean
    private ProgresoRepository progresoRepository;

    @MockitoBean
    private ModeloIARepository modeloIARepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("/actuator/health responde 200 UP sin token y sin detalles sensibles")
    void actuatorHealthPublicoSinDetalles() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("Otros endpoints de actuator no expuestos devuelven 403 o 404")
    void otrosEndpointsActuatorNoExpuestos() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CORS permite origen de CloudFront configurado para solicitudes del frontend")
    void corsPermiteOrigenCloudFront() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://d1234567890.cloudfront.net")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://d1234567890.cloudfront.net"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
