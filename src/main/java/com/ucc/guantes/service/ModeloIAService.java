package com.ucc.guantes.service;

import com.ucc.guantes.dto.ia.EstadisticasIAResponse;
import com.ucc.guantes.dto.ia.ModeloIARequest;
import com.ucc.guantes.dto.ia.ModeloIAResponse;
import com.ucc.guantes.exception.BadRequestException;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.ModeloIA;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.ModeloIARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio encargado de gestionar el ciclo de vida de los modelos de IA,
 * control de versiones activas y agregación estadística de inferencias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModeloIAService {

    private final ModeloIARepository modeloIARepository;
    private final IntentoPracticaRepository intentoPracticaRepository;

    /**
     * Registra una nueva versión de modelo de IA.
     * Si se marca como activo, desactiva automáticamente cualquier otra versión previamente activa.
     *
     * @param request Datos de la versión del modelo
     * @return ModeloIAResponse con los datos persistidos
     */
    @Transactional
    public ModeloIAResponse registrar(ModeloIARequest request) {
        if (modeloIARepository.existsByVersion(request.getVersion())) {
            throw new BadRequestException("Ya existe un modelo con la versión: " + request.getVersion());
        }

        // Si la nueva versión se registra como activa, desactivamos la versión actualmente activa
        if (Boolean.TRUE.equals(request.getActivo())) {
            modeloIARepository.findByActivoTrue().ifPresent(m -> {
                m.setActivo(false);
                modeloIARepository.save(m);
                log.info("Desactivando versión previa de modelo: {}", m.getVersion());
            });
        }

        ModeloIA modelo = ModeloIA.builder()
                .version(request.getVersion())
                .accuracy(request.getAccuracy())
                .numClases(request.getNumClases())
                .numMuestras(request.getNumMuestras())
                .descripcion(request.getDescripcion())
                .activo(request.getActivo() != null ? request.getActivo() : false)
                .build();

        modelo = modeloIARepository.save(modelo);
        log.info("Nueva versión de modelo IA registrada exitosamente: {} (Activo: {})",
                modelo.getVersion(), modelo.getActivo());

        return toResponse(modelo);
    }

    /**
     * Retorna la versión del modelo actualmente activa en la plataforma.
     *
     * @return ModeloIAResponse de la versión activa
     * @throws ResourceNotFoundException si no hay ningún modelo activo
     */
    @Transactional(readOnly = true)
    public ModeloIAResponse obtenerActivo() {
        return modeloIARepository.findByActivoTrue()
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ningún modelo de IA activo actualmente"));
    }

    /**
     * Lista todas las versiones de modelos registradas en el sistema.
     *
     * @return Lista de ModeloIAResponse
     */
    @Transactional(readOnly = true)
    public List<ModeloIAResponse> listar() {
        return modeloIARepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calcula métricas agregadas globales sobre el uso y rendimiento del modelo de IA en las prácticas.
     *
     * @return EstadisticasIAResponse con métricas calculadas
     */
    @Transactional(readOnly = true)
    public EstadisticasIAResponse obtenerEstadisticas() {
        Double promedio = intentoPracticaRepository.findPromedioConfianzaGlobal();
        Long totalIA = intentoPracticaRepository.countIntentosConIA();
        Long totalGeneral = intentoPracticaRepository.count();

        double tasa = (totalGeneral != null && totalGeneral > 0)
                ? (double) totalIA / totalGeneral
                : 0.0;

        return EstadisticasIAResponse.builder()
                .confianzaPromedioGlobal(promedio != null ? Math.round(promedio * 1000.0) / 1000.0 : 0.0)
                .totalIntentosConIA(totalIA != null ? totalIA : 0L)
                .totalIntentosTotales(totalGeneral != null ? totalGeneral : 0L)
                .tasaUsoIA(Math.round(tasa * 1000.0) / 1000.0)
                .build();
    }

    private ModeloIAResponse toResponse(ModeloIA modelo) {
        return ModeloIAResponse.builder()
                .id(modelo.getId())
                .version(modelo.getVersion())
                .accuracy(modelo.getAccuracy())
                .numClases(modelo.getNumClases())
                .numMuestras(modelo.getNumMuestras())
                .fechaEntrenamiento(modelo.getFechaEntrenamiento())
                .descripcion(modelo.getDescripcion())
                .activo(modelo.getActivo())
                .build();
    }
}
