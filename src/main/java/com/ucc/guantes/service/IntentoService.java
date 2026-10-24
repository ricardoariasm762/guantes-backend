package com.ucc.guantes.service;

import com.ucc.guantes.dto.intento.IntentoRequest;
import com.ucc.guantes.dto.intento.IntentoResponse;
import com.ucc.guantes.exception.ResourceNotFoundException;
import com.ucc.guantes.model.IntentoPractica;
import com.ucc.guantes.model.PasoCompletado;
import com.ucc.guantes.model.Usuario;
import com.ucc.guantes.repository.IntentoPracticaRepository;
import com.ucc.guantes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucc.guantes.model.FuentePrediccion;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentoService {

    private final IntentoPracticaRepository intentoPracticaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProgresoService progresoService;

    @Transactional
    public IntentoResponse registrarIntento(IntentoRequest request, String userEmail) {
        Usuario usuario;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", request.getUsuarioId()));
        } else if (userEmail != null && !userEmail.isBlank()) {
            usuario = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userEmail));
        } else {
            throw new IllegalArgumentException("Debe especificarse el usuario del intento");
        }

        IntentoPractica intento = IntentoPractica.builder()
                .usuario(usuario)
                .modoPractica(request.getModoPractica())
                .fechaInicio(request.getFechaInicio() != null ? request.getFechaInicio() : LocalDateTime.now())
                .fechaFin(request.getFechaFin())
                .puntaje(request.getPuntaje())
                .duracionSegundos(request.getDuracionSegundos())
                .completado(request.getCompletado() != null ? request.getCompletado() : false)
                .observaciones(request.getObservaciones())
                .confianzaPromedio(request.getConfianzaPromedio())
                .versionModelo(request.getVersionModelo())
                .detalleIA(request.getDetalleIA())
                .pasos(new ArrayList<>())
                .build();

        // Lógica clínica híbrida: Cálculo automático de fuentePrediccion si no fue enviada explícitamente por el cliente
        FuentePrediccion fuente = request.getFuentePrediccion();
        if (fuente == null && request.getConfianzaPromedio() != null) {
            double conf = request.getConfianzaPromedio();
            if (conf >= 0.8) {
                // Alta certeza de IA (>= 80%): Se clasifica como CONSENSO (alta concordancia con estándares clínicos)
                fuente = FuentePrediccion.CONSENSO;
            } else if (conf >= 0.6) {
                // Certeza moderada (60% - 80%): Se clasifica como MIXTO (arbitraje híbrido entre IA y reglas de técnica)
                fuente = FuentePrediccion.MIXTO;
            } else {
                // Baja certeza (< 60%): Predominan las REGLAS clínicas deterministas debido a la criticidad aséptica
                fuente = FuentePrediccion.REGLAS;
            }
        }
        intento.setFuentePrediccion(fuente);

        // Registro de telemetría de IA con SLF4J
        if (intento.getVersionModelo() != null || intento.getConfianzaPromedio() != null) {
            log.info("Intento registrado con metadatos de IA - Usuario: {}, Versión Modelo: {}, Confianza: {}, Fuente: {}",
                    usuario.getEmail(), intento.getVersionModelo(), intento.getConfianzaPromedio(), intento.getFuentePrediccion());

            if (intento.getConfianzaPromedio() != null && (intento.getConfianzaPromedio() < 0.0 || intento.getConfianzaPromedio() > 1.0)) {
                log.warn("Valor anómalo de confianzaPromedio registrado para el usuario {}: {}",
                        usuario.getEmail(), intento.getConfianzaPromedio());
            }
        }

        if (request.getPasos() != null && !request.getPasos().isEmpty()) {
            for (IntentoRequest.PasoRequest pReq : request.getPasos()) {
                PasoCompletado paso = PasoCompletado.builder()
                        .numeroPaso(pReq.getNumeroPaso())
                        .nombrePaso(pReq.getNombrePaso())
                        .descripcion(pReq.getDescripcion())
                        .exitoso(pReq.getExitoso() != null ? pReq.getExitoso() : true)
                        .tiempoSegundos(pReq.getTiempoSegundos())
                        .precision(pReq.getPrecision())
                        .observacionesError(pReq.getObservacionesError())
                        .fechaHora(pReq.getFechaHora() != null ? pReq.getFechaHora() : LocalDateTime.now())
                        .build();
                intento.addPaso(paso);
            }
        }

        IntentoPractica guardado = intentoPracticaRepository.save(intento);

        // Actualizar automáticamente las métricas de progreso del usuario
        progresoService.actualizarProgreso(usuario, guardado);

        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<IntentoResponse> listarTodos() {
        return intentoPracticaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntentoResponse> listarPorUsuario(Long usuarioId) {
        return intentoPracticaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntentoResponse> listarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", email));
        return listarPorUsuario(usuario.getId());
    }

    @Transactional(readOnly = true)
    public IntentoResponse obtenerPorId(Long id) {
        IntentoPractica intento = intentoPracticaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntentoPractica", "id", id));
        return mapToResponse(intento);
    }

    @Transactional
    public void eliminarIntento(Long id, String userEmail) {
        IntentoPractica intento = intentoPracticaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntentoPractica", "id", id));

        Usuario usuario = intento.getUsuario();
        if (userEmail != null && !userEmail.isBlank()) {
            Usuario invocador = usuarioRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userEmail));
            if (!invocador.getRol().name().equals("ADMIN") && !usuario.getId().equals(invocador.getId())) {
                throw new com.ucc.guantes.exception.BadRequestException("No tiene permisos para eliminar este intento");
            }
        }

        intentoPracticaRepository.delete(intento);
        progresoService.recalcularProgreso(usuario.getId());
    }

    private IntentoResponse mapToResponse(IntentoPractica intento) {
        List<IntentoResponse.PasoResponse> pasos = null;
        if (intento.getPasos() != null) {
            pasos = intento.getPasos().stream()
                    .map(p -> IntentoResponse.PasoResponse.builder()
                            .id(p.getId())
                            .numeroPaso(p.getNumeroPaso())
                            .nombrePaso(p.getNombrePaso())
                            .descripcion(p.getDescripcion())
                            .exitoso(p.getExitoso())
                            .tiempoSegundos(p.getTiempoSegundos())
                            .precision(p.getPrecision())
                            .observacionesError(p.getObservacionesError())
                            .fechaHora(p.getFechaHora())
                            .build())
                    .toList();
        }

        return IntentoResponse.builder()
                .id(intento.getId())
                .usuarioId(intento.getUsuario().getId())
                .usuarioNombre(intento.getUsuario().getNombre() + " " + intento.getUsuario().getApellido())
                .usuarioEmail(intento.getUsuario().getEmail())
                .modoPractica(intento.getModoPractica())
                .fechaInicio(intento.getFechaInicio())
                .fechaFin(intento.getFechaFin())
                .puntaje(intento.getPuntaje())
                .duracionSegundos(intento.getDuracionSegundos())
                .completado(intento.getCompletado())
                .observaciones(intento.getObservaciones())
                .confianzaPromedio(intento.getConfianzaPromedio())
                .fuentePrediccion(intento.getFuentePrediccion())
                .versionModelo(intento.getVersionModelo())
                .detalleIA(intento.getDetalleIA())
                .pasos(pasos)
                .build();
    }
}
