package com.ucc.guantes.controller;

import com.ucc.guantes.dto.ia.EstadisticasIAResponse;
import com.ucc.guantes.dto.ia.ModeloIARequest;
import com.ucc.guantes.dto.ia.ModeloIAResponse;
import com.ucc.guantes.service.ModeloIAService;
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

/**
 * Controlador REST para la gestión de versiones del modelo de Inteligencia Artificial (TensorFlow.js)
 * y consulta de métricas de telemetría de inferencia clínica.
 */
@RestController
@RequestMapping("/api/ia")
@RequiredArgsConstructor
@Tag(name = "Modelo IA", description = "Gestión de versiones del modelo de IA y estadísticas de inferencia")
@SecurityRequirement(name = "bearerAuth")
public class ModeloIAController {

    private final ModeloIAService modeloIAService;

    @PostMapping("/version")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una nueva versión del modelo", description = "Registra una versión de modelo TF.js entrenado. Si se marca como activo, desactiva la versión activa anterior (requiere rol ADMIN)")
    public ResponseEntity<ModeloIAResponse> registrar(@Valid @RequestBody ModeloIARequest request) {
        return new ResponseEntity<>(modeloIAService.registrar(request), HttpStatus.CREATED);
    }

    @GetMapping("/version/activa")
    @Operation(summary = "Obtener la versión activa del modelo", description = "Retorna los metadatos de la versión del modelo activa para que el frontend valide si tiene los pesos actualizados")
    public ResponseEntity<ModeloIAResponse> obtenerActiva() {
        return ResponseEntity.ok(modeloIAService.obtenerActivo());
    }

    @GetMapping("/versiones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las versiones registradas", description = "Retorna el historial completo de versiones de modelos de IA registradas (requiere rol ADMIN)")
    public ResponseEntity<List<ModeloIAResponse>> listar() {
        return ResponseEntity.ok(modeloIAService.listar());
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas agregadas de uso de IA", description = "Calcula indicadores consolidados de uso, confianza promedio global y tasa de adopción de IA")
    public ResponseEntity<EstadisticasIAResponse> estadisticas() {
        return ResponseEntity.ok(modeloIAService.obtenerEstadisticas());
    }
}
