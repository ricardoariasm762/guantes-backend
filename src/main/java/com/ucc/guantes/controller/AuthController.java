package com.ucc.guantes.controller;

import com.ucc.guantes.dto.auth.AuthResponse;
import com.ucc.guantes.dto.auth.LoginRequest;
import com.ucc.guantes.dto.auth.RegisterRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de registro, autenticación y obtención de perfil")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta para un estudiante, docente o administrador")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica con email y contraseña, retornando el token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/perfil")
    @Operation(summary = "Obtener perfil actual", description = "Retorna los datos del usuario autenticado actualmente")
    public ResponseEntity<UsuarioResponse> perfil(@AuthenticationPrincipal UserDetails userDetails) {
        UsuarioResponse response = authService.getPerfil(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
