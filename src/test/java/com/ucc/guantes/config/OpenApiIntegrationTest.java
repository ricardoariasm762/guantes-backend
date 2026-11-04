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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class OpenApiIntegrationTest {

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
    @DisplayName("/v3/api-docs responde 200 sin token e incluye bearerAuth JWT y etiquetas en español")
    void apiDocsAccesibleSinTokenYConBearerAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("API Herramienta Digital Guantes - UCC"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.tags", hasItems("Autenticación")))
                .andExpect(jsonPath("$.paths['/api/intentos'].get.tags", hasItems("Intentos de Práctica")));
    }

    @Test
    @DisplayName("/swagger-ui.html y /swagger-ui/index.html responden sin token JWT")
    void swaggerUiAccesibleSinToken() throws Exception {
        MvcResult redirectResult = mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = redirectResult.getResponse().getRedirectedUrl();
        mockMvc.perform(get(location != null ? location : "/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    @DisplayName("Rutas protegidas siguen exigiendo autenticación aunque Swagger sea público")
    void rutasProtegidasSiguenBloqueadasSinToken() throws Exception {
        mockMvc.perform(get("/api/intentos"))
                .andExpect(status().isForbidden());
    }
}
