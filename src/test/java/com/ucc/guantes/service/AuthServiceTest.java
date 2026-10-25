package com.ucc.guantes.service;

import com.ucc.guantes.dto.auth.AuthResponse;
import com.ucc.guantes.dto.auth.RegisterRequest;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import com.ucc.guantes.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ProgresoService progresoService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("El registro público debe forzar el rol ESTUDIANTE y crear progreso")
    void testRegistroPublicoFuerzaRolEstudiante() {
        // Un usuario intenta registrarse enviando Rol.ADMIN
        RegisterRequest request = RegisterRequest.builder()
                .nombre("Intento")
                .apellido("Admin")
                .email("admin_fake@ucc.edu.co")
                .password("password123")
                .rol(Rol.ADMIN)
                .build();

        when(usuarioRepository.existsByEmail("admin_fake@ucc.edu.co")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-token-fake");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Rol.ESTUDIANTE, response.getRol());

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioGuardado = usuarioCaptor.getValue();

        assertEquals(Rol.ESTUDIANTE, usuarioGuardado.getRol(), "El rol guardado debe ser forzado a ESTUDIANTE");
        verify(progresoService, times(1)).crearProgresoInicial(any(Usuario.class));
    }
}
