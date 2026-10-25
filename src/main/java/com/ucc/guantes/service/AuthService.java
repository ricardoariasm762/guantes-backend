package com.ucc.guantes.service;

import com.ucc.guantes.dto.auth.AuthResponse;
import com.ucc.guantes.dto.auth.LoginRequest;
import com.ucc.guantes.dto.auth.RegisterRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.Rol;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.UsuarioRepository;
import com.ucc.guantes.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final ProgresoService progresoService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo electrónico ya está registrado: " + request.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.ESTUDIANTE) // Forzar rol a ESTUDIANTE en registro público
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        progresoService.crearProgresoInicial(guardado);

        String token = jwtService.generateToken(guardado);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(guardado.getId())
                .nombre(guardado.getNombre())
                .apellido(guardado.getApellido())
                .email(guardado.getEmail())
                .rol(guardado.getRol())
                .build();
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        return register(request);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", request.getEmail()));

        String token = jwtService.generateToken(usuario);

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol())
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse getPerfil(String email) {
        return usuarioService.obtenerPorEmail(email);
    }
}
