package com.ucc.guantes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucc.guantes.dto.ia.EstadisticasIAResponse;
import com.ucc.guantes.dto.ia.ModeloIARequest;
import com.ucc.guantes.dto.ia.ModeloIAResponse;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.GlobalExceptionHandler;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.service.ModeloIAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ModeloIAControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ModeloIAService modeloIAService;

    @InjectMocks
    private ModeloIAController modeloIAController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(modeloIAController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/ia/version con payload válido debe responder 201 CREATED")
    void testRegistrarModeloExitoso() throws Exception {
        ModeloIARequest request = ModeloIARequest.builder()
                .version("v1.0-2026-10")
                .accuracy(0.95)
                .numClases(6)
                .numMuestras(15000)
                .descripcion("Modelo TF.js de prueba")
                .activo(true)
                .build();

        ModeloIAResponse response = ModeloIAResponse.builder()
                .id(1L)
                .version("v1.0-2026-10")
                .accuracy(0.95)
                .numClases(6)
                .numMuestras(15000)
                .fechaEntrenamiento(LocalDateTime.now())
                .descripcion("Modelo TF.js de prueba")
                .activo(true)
                .build();

        when(modeloIAService.registrar(any(ModeloIARequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ia/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.version").value("v1.0-2026-10"))
                .andExpect(jsonPath("$.accuracy").value(0.95))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("POST /api/ia/version con campos inválidos debe responder 400 BAD REQUEST")
    void testRegistrarModeloInvalido() throws Exception {
        ModeloIARequest requestInvalido = ModeloIARequest.builder()
                .version("") // Blank no permitido
                .accuracy(1.5) // Mayor a 1.0 no permitido
                .numClases(0) // Menor a 1 no permitido
                .numMuestras(-5) // Menor a 1 no permitido
                .build();

        mockMvc.perform(post("/api/ia/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validación"));
    }

    @Test
    @DisplayName("POST /api/ia/version cuando la versión ya existe debe responder 400 BAD REQUEST")
    void testRegistrarVersionDuplicadaRetornaBadRequest() throws Exception {
        ModeloIARequest request = ModeloIARequest.builder()
                .version("v1.0-2026-10")
                .accuracy(0.95)
                .numClases(6)
                .numMuestras(15000)
                .build();

        when(modeloIAService.registrar(any(ModeloIARequest.class)))
                .thenThrow(new BadRequestException("Ya existe un modelo con la versión: v1.0-2026-10"));

        mockMvc.perform(post("/api/ia/version")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe un modelo con la versión: v1.0-2026-10"));
    }

    @Test
    @DisplayName("GET /api/ia/version/activa debe responder 200 OK con el modelo activo")
    void testObtenerVersionActiva() throws Exception {
        ModeloIAResponse response = ModeloIAResponse.builder()
                .id(1L)
                .version("v1.0-2026-10")
                .accuracy(0.95)
                .numClases(6)
                .numMuestras(15000)
                .activo(true)
                .build();

        when(modeloIAService.obtenerActivo()).thenReturn(response);

        mockMvc.perform(get("/api/ia/version/activa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("v1.0-2026-10"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("GET /api/ia/version/activa cuando no hay activo debe responder 404 NOT FOUND")
    void testObtenerVersionActivaNoEncontrada() throws Exception {
        when(modeloIAService.obtenerActivo())
                .thenThrow(new ResourceNotFoundException("No hay ningún modelo de IA activo actualmente"));

        mockMvc.perform(get("/api/ia/version/activa"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay ningún modelo de IA activo actualmente"));
    }

    @Test
    @DisplayName("GET /api/ia/versiones debe responder 200 OK con la lista de modelos")
    void testListarVersiones() throws Exception {
        ModeloIAResponse response = ModeloIAResponse.builder()
                .id(1L)
                .version("v1.0-2026-10")
                .accuracy(0.95)
                .numClases(6)
                .numMuestras(15000)
                .activo(true)
                .build();

        when(modeloIAService.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ia/versiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].version").value("v1.0-2026-10"));
    }

    @Test
    @DisplayName("GET /api/ia/estadisticas debe responder 200 OK con métricas consolidadas")
    void testObtenerEstadisticas() throws Exception {
        EstadisticasIAResponse stats = EstadisticasIAResponse.builder()
                .confianzaPromedioGlobal(0.875)
                .totalIntentosConIA(120L)
                .totalIntentosTotales(150L)
                .tasaUsoIA(0.8)
                .build();

        when(modeloIAService.obtenerEstadisticas()).thenReturn(stats);

        mockMvc.perform(get("/api/ia/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confianzaPromedioGlobal").value(0.875))
                .andExpect(jsonPath("$.totalIntentosConIA").value(120L))
                .andExpect(jsonPath("$.tasaUsoIA").value(0.8));
    }

    @Test
    @DisplayName("Verificar que los endpoints sensibles tengan la anotación @PreAuthorize('hasRole(\\'ADMIN\\')')")
    void testAnotacionesDeSeguridadAdmin() throws NoSuchMethodException {
        Method registrarMethod = ModeloIAController.class.getMethod("registrar", ModeloIARequest.class);
        PreAuthorize preAuthRegistrar = registrarMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthRegistrar, "El método registrar debe tener @PreAuthorize");
        assertEquals("hasRole('ADMIN')", preAuthRegistrar.value());

        Method listarMethod = ModeloIAController.class.getMethod("listar");
        PreAuthorize preAuthListar = listarMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthListar, "El método listar debe tener @PreAuthorize");
        assertEquals("hasRole('ADMIN')", preAuthListar.value());

        // Verificar que obtenerActiva NO esté restringido a ADMIN para permitir acceso a estudiantes
        Method activaMethod = ModeloIAController.class.getMethod("obtenerActiva");
        PreAuthorize preAuthActiva = activaMethod.getAnnotation(PreAuthorize.class);
        assertNull(preAuthActiva, "El método obtenerActiva no debe requerir rol ADMIN");
    }
}
