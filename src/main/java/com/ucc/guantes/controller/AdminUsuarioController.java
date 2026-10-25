package com.ucc.guantes.controller;

import com.ucc.guantes.dto.usuario.UsuarioRequest;
import com.ucc.guantes.dto.usuario.UsuarioResponse;
import com.ucc.guantes.dto.usuario.UsuarioUpdateRequest;
import com.ucc.guantes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Administración de Usuarios", description = "Endpoints de gestión de usuarios exclusivos para el rol ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios", description = "Retorna la lista completa de usuarios registrados en el sistema")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @PostMapping
    @Operation(summary = "Crear nuevo usuario", description = "Permite a un administrador crear un usuario con cualquier rol (ESTUDIANTE, DOCENTE, ADMIN)")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario existente", description = "Actualiza los datos personales, académicos, rol o contraseña de un usuario")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", description = "Elimina permanentemente un usuario y en cascada su progreso e intentos de práctica")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna los detalles de un usuario específico según su ID")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }
}
