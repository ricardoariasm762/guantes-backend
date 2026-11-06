package com.ucc.guantes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ucc.guantes.dto.intento.IntentoRequest;
import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.model.IntentoPractica;
import com.ucc.guantes.model.ModoPractica;
import com.ucc.guantes.model.Progreso;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
class IntentoSeguridadTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Usuario usuarioA;
    private Usuario usuarioB;
    private Usuario docente;
    private IntentoPractica intentoDeB;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        usuarioA = Usuario.builder()
                .id(1L)
                .nombre("Estudiante A")
                .apellido("Perez")
                .email("estudianteA@ucc.edu.co")
                .password("$2a$10$encodedPasswordA")
                .semestre(3)
                .grupo("G1")
                .rol(Rol.ESTUDIANTE)
                .activo(true)
                .build();

        usuarioB = Usuario.builder()
                .id(2L)
                .nombre("Estudiante B")
                .apellido("Gomez")
                .email("estudianteB@ucc.edu.co")
                .password("$2a$10$encodedPasswordB")
                .semestre(3)
                .grupo("G1")
                .rol(Rol.ESTUDIANTE)
                .activo(true)
                .build();

        docente = Usuario.builder()
                .id(3L)
                .nombre("Docente Clinico")
                .apellido("Ramirez")
                .email("docente@ucc.edu.co")
                .password("$2a$10$encodedPasswordD")
                .rol(Rol.DOCENTE)
                .activo(true)
                .build();

        intentoDeB = IntentoPractica.builder()
                .id(100L)
                .usuario(usuarioB)
                .modoPractica(ModoPractica.GUIADO)
                .fechaInicio(LocalDateTime.now())
                .puntaje(90.0)
                .duracionSegundos(85)
                .completado(true)
                .pasos(new ArrayList<>())
                .build();

        when(usuarioRepository.findByEmail("estudianteA@ucc.edu.co")).thenReturn(Optional.of(usuarioA));
        when(usuarioRepository.findByEmail("estudianteB@ucc.edu.co")).thenReturn(Optional.of(usuarioB));
        when(usuarioRepository.findByEmail("docente@ucc.edu.co")).thenReturn(Optional.of(docente));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioA));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioB));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(docente));

        when(intentoPracticaRepository.findById(100L)).thenReturn(Optional.of(intentoDeB));
        when(intentoPracticaRepository.findByUsuarioIdOrderByFechaInicioDesc(any())).thenReturn(Collections.emptyList());

        when(progresoRepository.findByUsuarioId(any())).thenReturn(Optional.of(Progreso.builder()
                .usuario(usuarioB)
                .intentosTotales(0)
                .intentosExitosos(0)
                .tiempoTotalSegundos(0)
                .promedioPuntaje(0.0)
                .mejorPuntaje(0.0)
                .nivelAlcanzado("PRINCIPIANTE")
                .ultimoAcceso(LocalDateTime.now())
                .build()));
        when(progresoRepository.save(any(Progreso.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("1. ESTUDIANTE A pide GET /api/intentos/usuario/{idB} -> 403 Forbidden")
    void testEstudianteNoPuedeVerIntentosDeOtroUsuario() throws Exception {
        mockMvc.perform(get("/api/intentos/usuario/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("2. ESTUDIANTE A pide GET /api/intentos/usuario/{idA} -> 200 OK")
    void testEstudiantePuedeVerSusPropiosIntentos() throws Exception {
        mockMvc.perform(get("/api/intentos/usuario/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "docente@ucc.edu.co", roles = {"DOCENTE"})
    @DisplayName("3. DOCENTE pide GET /api/intentos/usuario/{idB} -> 200 OK")
    void testDocentePuedeVerIntentosDeCualquierUsuario() throws Exception {
        mockMvc.perform(get("/api/intentos/usuario/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("4. ESTUDIANTE A pide GET /api/intentos/{idIntentoDeB} -> 403 Forbidden")
    void testEstudianteNoPuedeVerIntentoDeOtro() throws Exception {
        mockMvc.perform(get("/api/intentos/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("5. ESTUDIANTE A hace POST /api/intentos con usuarioId = idB -> 403 Forbidden")
    void testEstudianteNoPuedeRegistrarIntentoParaOtroUsuario() throws Exception {
        IntentoRequest request = IntentoRequest.builder()
                .usuarioId(2L)
                .modoPractica(ModoPractica.GUIADO)
                .puntaje(80.0)
                .duracionSegundos(60)
                .build();

        mockMvc.perform(post("/api/intentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("6a. ESTUDIANTE A hace DELETE /api/intentos/{idIntentoDeB} -> 403 Forbidden")
    void testEstudianteNoPuedeEliminarIntentoDeOtro() throws Exception {
        mockMvc.perform(delete("/api/intentos/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "docente@ucc.edu.co", roles = {"DOCENTE"})
    @DisplayName("6b. DOCENTE hace DELETE /api/intentos/{idIntentoDeB} -> 204 No Content")
    void testDocentePuedeEliminarIntentoDeEstudiante() throws Exception {
        mockMvc.perform(delete("/api/intentos/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("7. ESTUDIANTE A pide GET /api/progreso/usuario/{idB} -> 403 Forbidden")
    void testEstudianteNoPuedeVerProgresoDeOtroUsuario() throws Exception {
        mockMvc.perform(get("/api/progreso/usuario/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("8. ESTUDIANTE A pide GET /api/usuarios/{idB} -> 403 Forbidden")
    void testEstudianteNoPuedeVerPerfilDeOtroUsuario() throws Exception {
        mockMvc.perform(get("/api/usuarios/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "estudianteA@ucc.edu.co", roles = {"ESTUDIANTE"})
    @DisplayName("9. ESTUDIANTE A hace PUT /api/usuarios/{idA} con rol=ADMIN -> el rol no cambia")
    void testEstudianteNoPuedeEscalarPrivilegiosARolAdmin() throws Exception {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("Estudiante A Modificado")
                .apellido("Perez")
                .email("estudianteA@ucc.edu.co")
                .password("nuevaPassword123")
                .semestre(4)
                .grupo("G2")
                .rol(Rol.ADMIN)
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.nombre").value("Estudiante A Modificado"));

        assertEquals(Rol.ESTUDIANTE, usuarioA.getRol(), "El rol en la entidad debe permanecer como ESTUDIANTE");
    }

    @Test
    @DisplayName("10. Zona horaria por defecto es America/Bogota al levantar el contexto")
    void testZonaHorariaEsAmericaBogota() {
        assertEquals("America/Bogota", TimeZone.getDefault().getID());
    }
}
