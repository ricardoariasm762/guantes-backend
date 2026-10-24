package com.ucc.guantes.controller;

import com.ucc.guantes.dto.intento.IntentoRequest;
import com.ucc.guantes.dto.intento.IntentoResponse;
import com.ucc.guantes.service.IntentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intentos")
@RequiredArgsConstructor
@Tag(name = "Intentos de Práctica", description = "Endpoints para registrar sesiones de práctica y consultar el historial")
public class IntentoController {

    private final IntentoService intentoService;

    @PostMapping
    @Operation(summary = "Registrar intento de práctica", description = "Guarda una nueva sesión de práctica con los guantes y sus pasos completados")
    public ResponseEntity<IntentoResponse> registrarIntento(
            @Valid @RequestBody IntentoRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        IntentoResponse response = intentoService.registrarIntento(request, email);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @Operation(summary = "Listar todos los intentos", description = "Obtiene todos los intentos registrados en la plataforma (requiere ADMIN o DOCENTE)")
    public ResponseEntity<List<IntentoResponse>> listarTodos() {
        return ResponseEntity.ok(intentoService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener intento por ID", description = "Retorna el detalle completo de un intento de práctica incluyendo los pasos")
    public ResponseEntity<IntentoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(intentoService.obtenerPorId(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Listar intentos por usuario", description = "Obtiene todos los intentos de práctica de un usuario específico")
    public ResponseEntity<List<IntentoResponse>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(intentoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/mis-intentos")
    @Operation(summary = "Listar mis intentos", description = "Obtiene el historial de intentos del usuario autenticado actualmente")
    public ResponseEntity<List<IntentoResponse>> misIntentos(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(intentoService.listarPorEmail(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar intento de práctica", description = "Elimina un intento registrado y recalcula automáticamente las estadísticas de progreso")
    public ResponseEntity<Void> eliminarIntento(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        intentoService.eliminarIntento(id, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.noContent().build();
    }
}
