package com.ucc.guantes.controller;

import com.ucc.guantes.dto.progreso.ProgresoResponse;
import com.ucc.guantes.service.ProgresoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progreso")
@RequiredArgsConstructor
@Tag(name = "Progreso", description = "Endpoints para el seguimiento del aprendizaje y progreso del estudiante")
public class ProgresoController {

    private final ProgresoService progresoService;

    @GetMapping("/mi-progreso")
    @Operation(summary = "Obtener mi progreso", description = "Retorna las métricas y estadísticas del estudiante autenticado")
    public ResponseEntity<ProgresoResponse> miProgreso(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(progresoService.obtenerProgresoPorEmail(userDetails.getUsername()));
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener progreso de un usuario", description = "Retorna el resumen de desempeño de un estudiante específico")
    public ResponseEntity<ProgresoResponse> obtenerPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(progresoService.obtenerProgresoPorUsuario(usuarioId));
    }

    @PostMapping("/recalcular/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    @Operation(summary = "Recalcular progreso", description = "Fuerza el recálculo de las métricas de un estudiante según sus intentos registrados")
    public ResponseEntity<ProgresoResponse> recalcular(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(progresoService.recalcularProgreso(usuarioId));
    }
}
