package com.ucc.guantes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.dto.usuario.UsuarioUpdateRequest;
import com.ucc.guantes.exception.GlobalExceptionHandler;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.service.UsuarioService;
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
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private AdminUsuarioController adminUsuarioController;

    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUsuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        usuarioResponse = UsuarioResponse.builder()
                .id(1L)
                .nombre("Maria Gomez")
                .email("maria@ucc.edu.co")
                .semestre(5)
                .grupo("G-1")
                .rol(Rol.DOCENTE)
                .fechaRegistro(LocalDateTime.now())
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Debe tener anotación @PreAuthorize(\"hasRole('ADMIN')\") a nivel de clase")
    void testAnotacionSeguridadClase() {
        PreAuthorize preAuthorize = AdminUsuarioController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "La clase AdminUsuarioController debe tener @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('ADMIN')"), "Debe exigir rol ADMIN");

        RequestMapping requestMapping = AdminUsuarioController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertTrue(requestMapping.value()[0].contains("/api/admin/usuarios"));
    }

    @Test
    @DisplayName("GET /api/admin/usuarios debe retornar lista de usuarios")
    void testListarTodos() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(usuarioResponse));

        mockMvc.perform(get("/api/admin/usuarios")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Maria Gomez"))
                .andExpect(jsonPath("$[0].email").value("maria@ucc.edu.co"))
                .andExpect(jsonPath("$[0].rol").value("DOCENTE"));
    }

    @Test
    @DisplayName("POST /api/admin/usuarios debe crear usuario exitosamente")
    void testCrearUsuario() throws Exception {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("Carlos Perez")
                .email("carlos@ucc.edu.co")
                .password("Password123")
                .semestre(4)
                .grupo("G-2")
                .rol(Rol.ESTUDIANTE)
                .build();

        when(usuarioService.crearUsuario(any(UsuarioRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(post("/api/admin/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/admin/usuarios/{id} debe actualizar usuario")
    void testActualizarUsuario() throws Exception {
        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .nombre("Maria Actualizada")
                .email("maria_actualizada@ucc.edu.co")
                .semestre(6)
                .grupo("G-1B")
                .rol(Rol.DOCENTE)
                .password("NuevaClave123")
                .build();

        when(usuarioService.actualizarUsuario(eq(1L), any(UsuarioUpdateRequest.class))).thenReturn(usuarioResponse);

        mockMvc.perform(put("/api/admin/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/admin/usuarios/{id} debe eliminar usuario")
    void testEliminarUsuario() throws Exception {
        mockMvc.perform(delete("/api/admin/usuarios/1"))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminarUsuario(1L);
    }

    @Test
    @DisplayName("GET /api/admin/usuarios/{id} debe retornar usuario por ID")
    void testObtenerPorId() throws Exception {
        when(usuarioService.obtenerPorId(1L)).thenReturn(usuarioResponse);

        mockMvc.perform(get("/api/admin/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Maria Gomez"));
    }

    @Test
    @DisplayName("GET /api/admin/usuarios/{id} retorna 404 si el usuario no existe")
    void testObtenerPorIdNoExiste() throws Exception {
        when(usuarioService.obtenerPorId(99L)).thenThrow(new ResourceNotFoundException("Usuario", "id", 99L));

        mockMvc.perform(get("/api/admin/usuarios/99"))
                .andExpect(status().isNotFound());
    }
}
