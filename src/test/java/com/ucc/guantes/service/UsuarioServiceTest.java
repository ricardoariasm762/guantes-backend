package com.ucc.guantes.service;

import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.dto.usuario.UsuarioUpdateRequest;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProgresoService progresoService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@ucc.edu.co")
                .password("encoded_pass")
                .semestre(3)
                .grupo("G-A")
                .rol(Rol.ESTUDIANTE)
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listarTodos debe retornar lista mapeada")
    void testListarTodos() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponse> lista = usuarioService.listarTodos();

        assertEquals(1, lista.size());
        assertEquals("juan@ucc.edu.co", lista.get(0).getEmail());
        assertEquals(3, lista.get(0).getSemestre());
        assertEquals("G-A", lista.get(0).getGrupo());
    }

    @Test
    @DisplayName("crearUsuario debe encriptar clave y crear progreso inicial")
    void testCrearUsuario() {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("Carlos Gomez")
                .email("carlos@ucc.edu.co")
                .password("plain_pass123")
                .semestre(2)
                .grupo("G-B")
                .rol(Rol.DOCENTE)
                .build();

        when(usuarioRepository.existsByEmail("carlos@ucc.edu.co")).thenReturn(false);
        when(passwordEncoder.encode("plain_pass123")).thenReturn("encoded_pass123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(2L);
            u.setFechaCreacion(LocalDateTime.now());
            return u;
        });

        UsuarioResponse response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("carlos@ucc.edu.co", response.getEmail());
        assertEquals(Rol.DOCENTE, response.getRol());
        verify(progresoService, times(1)).crearProgresoInicial(any(Usuario.class));
    }

    @Test
    @DisplayName("crearUsuario falla si email ya existe")
    void testCrearUsuarioEmailExistente() {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("Carlos Gomez")
                .email("juan@ucc.edu.co")
                .password("plain_pass123")
                .rol(Rol.ESTUDIANTE)
                .build();

        when(usuarioRepository.existsByEmail("juan@ucc.edu.co")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> usuarioService.crearUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarUsuario actualiza datos y contraseña si viene en request")
    void testActualizarUsuarioConPassword() {
        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .nombre("Juan Modificado")
                .email("juan@ucc.edu.co")
                .semestre(4)
                .grupo("G-C")
                .rol(Rol.ADMIN)
                .password("nuevaClave123")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("encoded_nuevaClave");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse response = usuarioService.actualizarUsuario(1L, request);

        assertEquals("Juan Modificado", response.getNombre());
        assertEquals(Rol.ADMIN, response.getRol());
        assertEquals(4, response.getSemestre());
        assertEquals("G-C", response.getGrupo());
        verify(passwordEncoder, times(1)).encode("nuevaClave123");
    }

    @Test
    @DisplayName("actualizarUsuario no encripta si password es nula")
    void testActualizarUsuarioSinPassword() {
        UsuarioUpdateRequest request = UsuarioUpdateRequest.builder()
                .nombre("Juan Sin Pass")
                .email("juan@ucc.edu.co")
                .semestre(4)
                .grupo("G-C")
                .rol(Rol.ESTUDIANTE)
                .password(null)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse response = usuarioService.actualizarUsuario(1L, request);

        assertEquals("Juan Sin Pass", response.getNombre());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("eliminarUsuario invoca delete en el repositorio")
    void testEliminarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        doNothing().when(usuarioRepository).delete(usuario);

        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository, times(1)).delete(usuario);
    }

    @Test
    @DisplayName("obtenerPorId retorna 404 si no existe")
    void testObtenerPorIdNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.obtenerPorId(99L));
    }
}
